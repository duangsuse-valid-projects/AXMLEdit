package cn.wjdiankong.axmledit.helper;

import cn.wjdiankong.axmledit.chunk.*;

import java.util.ArrayList;
import java.util.List;

public class ChunkParser {

    public static int stringChunkOffset = 8;
    public static int resourceChunkOffset;
    public static int nextChunkOffset;

    public static XmlStruct xmlStruct = new XmlStruct();

    public static boolean isApplication = false;
    public static boolean isManifest = false;

    public static List<TagChunk> tagChunkList = new ArrayList<>();

    public static void clear() {
        resourceChunkOffset = 0;
        nextChunkOffset = 0;
        isApplication = false;
        isManifest = false;
        tagChunkList.clear();
        xmlStruct.clear();
    }

    public static void parserXml() {
        clear();
        ChunkParser.parserXmlHeader(xmlStruct.byteSrc);
        ChunkParser.parserStringChunk(xmlStruct.byteSrc);
        ChunkParser.parserResourceChunk(xmlStruct.byteSrc);
        ChunkParser.parserXmlContent(xmlStruct.byteSrc);
    }

    /**
     * 解析xml的头部信息
     *
     * @param byteSrc 需要解析的 xml 字节数组
     */
    public static void parserXmlHeader(byte[] byteSrc) {
        byte[] xmlMagic = Utils.copyByte(byteSrc, 0, 4);
        byte[] xmlSize = Utils.copyByte(byteSrc, 4, 4);
        xmlStruct.magicNumber = xmlMagic;
        xmlStruct.fileSize = xmlSize;
    }

    /**
     * 解析 StringChunk
     *
     * @param byteSrc 需要解析的 xml 字节数组
     */
    public static void parserStringChunk(byte[] byteSrc) {
        xmlStruct.stringChunk = StringChunk.createChunk(byteSrc, stringChunkOffset);
        byte[] chunkSizeByte = Utils.copyByte(byteSrc, 12, 4);
        resourceChunkOffset = stringChunkOffset + Utils.byte2int(chunkSizeByte);
    }

    /**
     * 解析 Resource Chunk
     *
     * @param byteSrc 需要解析的 xml 字节数组
     */
    public static void parserResourceChunk(byte[] byteSrc) {
        xmlStruct.resChunk = ResourceChunk.createChunk(byteSrc, resourceChunkOffset);
        byte[] chunkSizeByte = Utils.copyByte(byteSrc, resourceChunkOffset + 4, 4);
        int chunkSize = Utils.byte2int(chunkSizeByte);
        nextChunkOffset = (resourceChunkOffset + chunkSize);
        AXmlEditor.tagStartChunkOffset = nextChunkOffset;

    }

    /**
     * 解析 StartNamespace Chunk
     *
     * @param byteSrc 需要解析的 xml 字节数组
     */
    public static void parserStartNamespaceChunk(byte[] byteSrc) {
        xmlStruct.startNamespaceChunk = StartNameSpaceChunk.createChunk(byteSrc);
    }

    /**
     * 解析 EndNamespace Chunk
     *
     * @param byteSrc 需要解析的 xml 字节数组
     */
    public static void parserEndNamespaceChunk(byte[] byteSrc) {
        xmlStruct.endNamespaceChunk = EndNameSpaceChunk.createChunk(byteSrc);
    }

    /**
     * 解析 StartTag Chunk
     *
     * @param byteSrc 需要解析的 xml 字节数组
     */
    public static void parserStartTagChunk(byte[] byteSrc, int offset) {

        StartTagChunk tagChunk = StartTagChunk.createChunk(byteSrc, offset);
        xmlStruct.startTagChunkList.add(tagChunk);
        TagChunk chunk = new TagChunk();
        chunk.startTagChunk = tagChunk;
        tagChunkList.add(chunk);

        // 解析 TagName
        byte[] tagNameByte = Utils.copyByte(byteSrc, 20, 4);
        int tagNameIndex = Utils.byte2int(tagNameByte);
        String tagName = xmlStruct.stringChunk.stringContentList.get(tagNameIndex);

        // 标记是否为 application 标签
        if (tagName.equals("application")) {
            isApplication = true;
        }

    }

    /**
     * 解析 EndTag Chunk
     *
     * @param byteSrc 需要解析的 xml 字节数组
     */
    public static void parserEndTagChunk(byte[] byteSrc, int offset) {
        EndTagChunk tagChunk = EndTagChunk.createChunk(byteSrc, offset);
        TagChunk chunk = tagChunkList.remove(tagChunkList.size() - 1);
        chunk.endTagChunk = tagChunk;
        xmlStruct.endTagChunkList.add(tagChunk);
        xmlStruct.tagChunkList.add(chunk); // 标签结束了, 需要把标签放入池中
    }

    /**
     * 解析 Text Chunk
     *
     * @param byteSrc 需要解析的 xml 字节数组
     */
    @SuppressWarnings("unused")
    public static void parserTextChunk(byte[] byteSrc) {
        xmlStruct.textChunkList.add(TextChunk.createChunk(byteSrc));
    }

    /**
     * 开始解析 xml 的正文内容 Chunk
     *
     * @param byteSrc 需要解析的 xml 字节数组
     */
    public static void parserXmlContent(byte[] byteSrc) {
        while (!isEnd(byteSrc.length)) {
            byte[] chunkTagByte = Utils.copyByte(byteSrc, nextChunkOffset, 4);
            byte[] chunkSizeByte = Utils.copyByte(byteSrc, nextChunkOffset + 4, 4);
            int chunkTag = Utils.byte2int(chunkTagByte);
            int chunkSize = Utils.byte2int(chunkSizeByte);
            switch (chunkTag) {
                case ChunkTypeNumber.CHUNK_STARTNS:
                    parserStartNamespaceChunk(Utils.copyByte(byteSrc, nextChunkOffset, chunkSize));
                    isManifest = true;
                    break;
                case ChunkTypeNumber.CHUNK_STARTTAG:
                    parserStartTagChunk(Utils.copyByte(byteSrc, nextChunkOffset, chunkSize), nextChunkOffset);
                    // 是否为 application 标签
                    if (isApplication) {
                        AXmlEditor.subAppTagChunkOffset = nextChunkOffset + chunkSize;
                        isApplication = false;
                    }
                    // 是否为 manifest 标签
                    if (isManifest) {
                        AXmlEditor.subTagChunkOffsets = nextChunkOffset + chunkSize;
                        isManifest = false;
                    }
                    break;
                case ChunkTypeNumber.CHUNK_ENDTAG:
                    parserEndTagChunk(Utils.copyByte(byteSrc, nextChunkOffset, chunkSize), nextChunkOffset);
                    break;
                case ChunkTypeNumber.CHUNK_ENDNS:
                    parserEndNamespaceChunk(Utils.copyByte(byteSrc, nextChunkOffset, chunkSize));
                    break;
            }
            nextChunkOffset += chunkSize;
        }

    }

    /**
     * 判断是否到文件结束位置了
     *
     * @param totalLen 文件总长度
     * @return 如果已经到达文件末尾, 返回 true
     */
    public static boolean isEnd(int totalLen) {
        return nextChunkOffset >= totalLen;
    }

}
