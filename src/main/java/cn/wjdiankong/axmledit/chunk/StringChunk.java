package cn.wjdiankong.axmledit.chunk;

import cn.wjdiankong.axmledit.helper.Utils;

import java.util.ArrayList;

import static cn.wjdiankong.axmledit.helper.Utils.addByte;

public class StringChunk {

    public byte[] type;
    public byte[] size;
    public byte[] strCount;
    public byte[] styleCount;
    public byte[] unknown;
    public byte[] strPoolOffset;
    public byte[] stylePoolOffset;
    public byte[] strOffsets;
    public byte[] styleOffsets;
    public byte[] strPool;
    public byte[] stylePool;

    public ArrayList<String> stringContentList;

    public static StringChunk createChunk(byte[] byteSrc, int stringChunkOffset) {

        StringChunk chunk = new StringChunk();

        // String Chunk 的标示
        chunk.type = Utils.copyByte(byteSrc, stringChunkOffset, 4);

        // String Size
        chunk.size = Utils.copyByte(byteSrc, 4 + stringChunkOffset, 4);
        int chunkSize = Utils.byte2int(chunk.size);

        // String Count
        chunk.strCount = Utils.copyByte(byteSrc, 8 + stringChunkOffset, 4);
        int chunkStringCount = Utils.byte2int(chunk.strCount);

        chunk.stringContentList = new ArrayList<>(chunkStringCount);

        // Style Count
        chunk.styleCount = Utils.copyByte(byteSrc, 12 + stringChunkOffset, 4);
        int chunkStyleCount = Utils.byte2int(chunk.styleCount);

        // unknown
        chunk.unknown = Utils.copyByte(byteSrc, 16 + stringChunkOffset, 4);

        // 这里需要注意的是, 后面的四个字节是 Style 的内容, 然后紧接着的四个字节始终是 0, 所以我们需要直接过滤这 8 个字节
        // String Offset 相对于 String Chunk 的起始位置 0x00000008
        chunk.strPoolOffset = Utils.copyByte(byteSrc, 20 + stringChunkOffset, 4);

        // Style Offset
        chunk.stylePoolOffset = Utils.copyByte(byteSrc, 24 + stringChunkOffset, 4);

        // String Offsets
        chunk.strOffsets = Utils.copyByte(byteSrc, 28 + stringChunkOffset, 4 * chunkStringCount);

        // Style Offsets
        chunk.styleOffsets = Utils.copyByte(byteSrc, 28 + stringChunkOffset + 4 * chunkStringCount, 4 * chunkStyleCount);

        int stringContentStart = 8 + Utils.byte2int(chunk.strPoolOffset);

        // String Content
        byte[] chunkStringContentByte = Utils.copyByte(byteSrc, stringContentStart, chunkSize);

        // 这里的格式是: 偏移值开始的两个字节是字符串的长度, 接着是字符串的内容, 后面跟着两个字符串的结束符 \00
        byte[] firstStringSizeByte = Utils.copyByte(chunkStringContentByte, 0, 2);
        // 一个字符对应两个字节
        int firstStringSize = Utils.byte2Short(firstStringSizeByte) * 2;
        byte[] firstStringContentByte = Utils.copyByte(chunkStringContentByte, 2, firstStringSize + 2);

        String firstStringContent = new String(firstStringContentByte);

        chunk.stringContentList.add(Utils.filterStringNull(firstStringContent));
        // 将字符串都放到 ArrayList 中
        int endStringIndex = 2 + firstStringSize + 2;
        while (chunk.stringContentList.size() < chunkStringCount) {
            // 一个字符对应两个字节, 所以要乘以 2
            int stringSize = Utils.byte2Short(Utils.copyByte(chunkStringContentByte, endStringIndex, 2)) * 2;
            byte[] temp = Utils.copyByte(chunkStringContentByte, endStringIndex + 2, stringSize + 2);
            String str = new String(temp);
            chunk.stringContentList.add(Utils.filterStringNull(str));
            endStringIndex += (2 + stringSize + 2);
        }

        int len = 0;
        for (String str : chunk.stringContentList) {
            len += 2;
            len += str.length() * 2;
            len += 2;
        }
        chunk.strPool = Utils.copyByte(byteSrc, stringContentStart, len);
        int stylePool = stringContentStart + len;

        chunk.stylePool = Utils.copyByte(byteSrc, stylePool, chunkSize - (stylePool));

        return chunk;
    }

    public byte[] getByte(ArrayList<String> strList) {

        byte[] strB = getStrListByte(strList);

        byte[] src = new byte[0];

        src = addByte(src, type);
        src = addByte(src, size);
        src = addByte(src, Utils.int2Byte(strList.size())); // 字符个数
        src = addByte(src, styleCount);
        src = addByte(src, unknown);
        src = addByte(src, strPoolOffset);
        src = addByte(src, stylePoolOffset);

        byte[] strOffsets = new byte[0];
        ArrayList<String> convertList = convertStrList(strList);

        int len = 0;
        for (String aConvertList : convertList) {
            strOffsets = addByte(strOffsets, Utils.int2Byte(len));
            len += (aConvertList.length() + 4); // 这里的 4 包括字符串头部的字符串长度 2 个字节, 和字符串结尾的 2 个字节
        }

        src = addByte(src, strOffsets); // 写入 string offsets 值

        int newStyleOffsets = src.length; // 写完 strOffsets 之后就是 styleOffsets 的值

        src = addByte(src, styleOffsets); // 写入style offsets 值

        int newStringPools = src.length;

        src = addByte(src, strB); // 写入 string pools

        src = addByte(src, stylePool); // 写入 style pools

        // 因为 strOffsets 大小的改变, 这里的 styleOffsets 也需要变动
        if (styleOffsets != null && styleOffsets.length > 0) {
            // 只有 style 有效才能写入
            src = Utils.replaceBytes(src, Utils.int2Byte(newStyleOffsets), 28 + strList.size() * 4);
        }

        // 因为 strOffsets 大小改变, 这里的 strPoolOffsets 和 stylePoolOffset 也要变动
        src = Utils.replaceBytes(src, Utils.int2Byte(newStringPools), 20); // 修改 strPoolOffsets 偏移值

        // 对于 String Chunk 的大小必须是 4 的倍数, 如果不是补齐, 因为 Chunk 一定是 2 的倍数, 所以只需要补齐 2 个字节即可
        if (src.length % 4 != 0) {
            src = addByte(src, new byte[]{0, 0});
        }

        // 修改 chunk 最终的大小
        src = Utils.replaceBytes(src, Utils.int2Byte(src.length), 4);

        return src;
    }

    @SuppressWarnings("unused")
    public int getLen() {
        return type.length + size.length + strCount.length + styleCount.length +
                unknown.length + strPoolOffset.length + stylePoolOffset.length +
                strOffsets.length + styleOffsets.length + strPool.length + stylePool.length;
    }

    private byte[] getStrListByte(ArrayList<String> strList) {
        byte[] src = new byte[0];
        ArrayList<String> stringContentList = convertStrList(strList);
        for (String aStringContentList : stringContentList) {
            byte[] tempAry = new byte[0];
            short len = (short) (aStringContentList.length() / 2);
            byte[] lenAry = Utils.shortToByte(len);
            tempAry = addByte(tempAry, lenAry);
            tempAry = addByte(tempAry, aStringContentList.getBytes());
            tempAry = addByte(tempAry, new byte[]{0, 0});
            src = addByte(src, tempAry);
        }
        return src;
    }

    private ArrayList<String> convertStrList(ArrayList<String> stringContentList) {
        ArrayList<String> destList = new ArrayList<>(stringContentList.size());
        for (String str : stringContentList) {
            byte[] temp = str.getBytes();
            byte[] src = new byte[temp.length * 2];
            for (int i = 0; i < temp.length; i++) {
                src[i * 2] = temp[i];
                src[i * 2 + 1] = 0;
            }
            destList.add(new String(src));
        }
        return destList;
    }

}
