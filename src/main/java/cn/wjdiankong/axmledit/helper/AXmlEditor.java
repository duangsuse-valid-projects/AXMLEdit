package cn.wjdiankong.axmledit.helper;

import cn.wjdiankong.axmledit.chunk.*;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class AXmlEditor {

    private static int tagStartChunkOffset,
            tagEndChunkOffset,
            subAppTagChunkOffset,
            subTagChunkOffsets;

    private static String[] isNotAppTag = new String[] {
            "uses-permission", "uses-sdk", "compatible-screens", "instrumentation", "library",
            "original-package", "package-verifier", "permission", "permission-group", "permission-tree",
            "protected-broadcast", "resource-overlay", "supports-input", "supports-screens", "upgrade-key-set",
            "uses-configuration", "uses-feature" };

    private static String prefixStr = "http://schemas.android.com/apk/res/android";
    private static int attrValue;
    static String nameAttrValue;

    /**
     * 删除 Axml 标签
     *
     * @param tagName 标签名
     */
    public static void removeTag(String tagName) {

        ChunkParser.parserXml();
        for (TagChunk tag : ChunkParser.xmlStruct.tagChunkList) {
            int tagNameIndex = Utils.byte2int(tag.startTagChunk.name); // string pool 索引
            String tagNameTmp = ChunkParser.xmlStruct.stringChunk.stringContentList.get(tagNameIndex); // 解引用
            if (tagName.equals(tagNameTmp)) { // 如果是要找的 tag
                for (AttributeData attrData : tag.startTagChunk.attrList) {
                    String attrName = ChunkParser.xmlStruct.stringChunk.stringContentList.get(attrData.name); // 属性名称
                    if ("name".equals(attrName)) {
                        String value = ChunkParser.xmlStruct.stringChunk.stringContentList.get(attrData.valueString); // <a name="foo"></a>
                        if (nameAttrValue.equals(value)) {
                            // 找到指定的 tag 开始删除
                            int size = Utils.byte2int(tag.endTagChunk.size);
                            int delStart = tag.startTagChunk.offset;
                            int delSize = (tag.endTagChunk.offset - tag.startTagChunk.offset) + size;
                            ChunkParser.xmlStruct.byteSrc = Utils.removeByte(ChunkParser.xmlStruct.byteSrc, delStart, delSize);

                            modifyFileSize();
                            return;
                        }
                    }
                }
            }
        }

    }

    /**
     * 添加标签内容
     */
    public static void addTag(String insertXml) {

        ChunkParser.parserXml();
        try {
            XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser pullParser = pullParserFactory.newPullParser();
            pullParser.setInput(new FileInputStream(insertXml), "UTF-8");
            int event = pullParser.getEventType();
            // 若为解析到末尾
            while (event != XmlPullParser.END_DOCUMENT) { // 文档结束
                // 节点名称
                switch (event) {

                    case XmlPullParser.START_DOCUMENT: // 文档开始
                        break;

                    case XmlPullParser.START_TAG: // 标签开始
                        String tagName = pullParser.getName();
                        int name = getStrIndex(tagName);
                        int attCount = pullParser.getAttributeCount();
                        byte[] attribute = new byte[20 * attCount];
                        for (int i = 0; i < pullParser.getAttributeCount(); i++) {
                            int attr_uri = getStrIndex(prefixStr);
                            // 这里需要对属性名做分离
                            String attrName = pullParser.getAttributeName(i);
                            String[] strAry = attrName.split(":");
                            int[] type = getAttrType(pullParser.getAttributeValue(i));
                            int attr_name = getStrIndex(strAry[1]);
                            int attr_value = getStrIndex(pullParser.getAttributeValue(i));
                            int attr_type = type[0];
                            int attr_data = type[1];
                            AttributeData data = AttributeData.createAttribute(attr_uri, attr_name, attr_value, attr_type, attr_data);
                            attribute = Utils.byteConcat(attribute, data.getBytes(), data.getLen() * i);
                        }

                        StartTagChunk startChunk = StartTagChunk.createChunk(name, attCount, -1, attribute);
                        // 构造一个新的 chunk 之后, 开始写入
                        if (isNotAppTag(tagName)) {
                            ChunkParser.xmlStruct.byteSrc = Utils.insertByte(ChunkParser.xmlStruct.byteSrc, subTagChunkOffsets, startChunk.getChunkBytes());
                            subTagChunkOffsets += startChunk.getChunkBytes().length;
                        } else {
                            ChunkParser.xmlStruct.byteSrc = Utils.insertByte(ChunkParser.xmlStruct.byteSrc, subAppTagChunkOffset, startChunk.getChunkBytes());
                            subAppTagChunkOffset += startChunk.getChunkBytes().length;
                        }
                        break;

                    case XmlPullParser.END_TAG: // 标签结束
                        tagName = pullParser.getName();
                        name = getStrIndex(tagName);
                        EndTagChunk endChunk = EndTagChunk.createChunk(name);
                        if (isNotAppTag(tagName)) {
                            ChunkParser.xmlStruct.byteSrc = Utils.insertByte(ChunkParser.xmlStruct.byteSrc, subTagChunkOffsets, endChunk.getChunkBytes());
                            subTagChunkOffsets += endChunk.getChunkBytes().length;
                        } else {
                            ChunkParser.xmlStruct.byteSrc = Utils.insertByte(ChunkParser.xmlStruct.byteSrc, subAppTagChunkOffset, endChunk.getChunkBytes());
                            subAppTagChunkOffset += endChunk.getChunkBytes().length;
                        }
                        break;

                }
                event = pullParser.next(); // 下一个标签
            }
        } catch (XmlPullParserException | IOException e) {
            System.err.println("Xml parsing error:" + e.toString());
        }

        modifyStringChunk();

        modifyFileSize();

    }

    /**
     * 删除属性
     *
     * @param tagName  标签名
     * @param attrName 属性名
     */
    public static void removeAttr(String tagName, String attrName) {

        ChunkParser.parserXml();
        for (StartTagChunk chunk : ChunkParser.xmlStruct.startTagChunkList) {
            int tagNameIndex = Utils.byte2int(chunk.name);
            String tagNameTmp = ChunkParser.xmlStruct.stringChunk.stringContentList.get(tagNameIndex);

            if (tagName.equals(tagNameTmp)) {

                // 如果是 application, manifest 标签直接处理就好
                if (tagName.equals("application") || tagName.equals("manifest") || tagName.equals("uses-sdk")) {
                    for (AttributeData data : chunk.attrList) {
                        String attrNameTemp1 = ChunkParser.xmlStruct.stringChunk.stringContentList.get(data.name);
                        if (attrName.equals(attrNameTemp1)) {
                            // 如果找到对应的标签, 发现只有一个属性值, 并且删除成功, 同时还得把这个标签给删除了
                            if (chunk.attrList.size() == 1) {
                                removeTag(tagName);
                                return;
                            }
                            // 还得修改对应的 tag chunk 中属性个个数和大小
                            int countStart = chunk.offset + 28;
                            byte[] modifyByte = Utils.int2Byte(chunk.attrList.size() - 1);
                            ChunkParser.xmlStruct.byteSrc = Utils.replaceBytes(ChunkParser.xmlStruct.byteSrc, modifyByte, countStart);

                            // 修改 chunk 的大小
                            int chunkSizeStart = chunk.offset + 4;
                            int chunkSize = Utils.byte2int(chunk.size);
                            byte[] modifyByteSize = Utils.int2Byte(chunkSize - 20); // 一个属性块是 20 个字节
                            ChunkParser.xmlStruct.byteSrc = Utils.replaceBytes(ChunkParser.xmlStruct.byteSrc, modifyByteSize, chunkSizeStart);

                            // 删除属性内容
                            int delStart = data.offset;
                            int delSize = data.getLen();
                            ChunkParser.xmlStruct.byteSrc = Utils.removeByte(ChunkParser.xmlStruct.byteSrc, delStart, delSize);

                            modifyFileSize();
                            return;
                        }
                    }
                }

                // 否则需要通过 name 找到指定的 tag
                for (AttributeData attrData : chunk.attrList) {
                    String attrNameTemp = ChunkParser.xmlStruct.stringChunk.stringContentList.get(attrData.name);
                    if ("name".equals(attrNameTemp)) { // 得先找到 tag 对应的唯一名称
                        String value = ChunkParser.xmlStruct.stringChunk.stringContentList.get(attrData.valueString);
                        if (nameAttrValue.equals(value)) {
                            for (AttributeData data : chunk.attrList) {
                                String attrNameTemp1 = ChunkParser.xmlStruct.stringChunk.stringContentList.get(data.name);
                                if (attrName.equals(attrNameTemp1)) {

                                    // 如果找到对应的标签, 发现只有一个属性值, 并且删除成功, 同时还得把这个标签给删除了
                                    if (chunk.attrList.size() == 1) {
                                        removeTag(tagName);
                                        return;
                                    }

                                    // 还得修改对应的 tag chunk 中属性个个数和大小
                                    int countStart = chunk.offset + 28;
                                    byte[] modifyByte = Utils.int2Byte(chunk.attrList.size() - 1);
                                    ChunkParser.xmlStruct.byteSrc = Utils.replaceBytes(ChunkParser.xmlStruct.byteSrc, modifyByte, countStart);

                                    // 修改 chunk 的大小
                                    int chunkSizeStart = chunk.offset + 4;
                                    int chunkSize = Utils.byte2int(chunk.size);
                                    byte[] modifyByteSize = Utils.int2Byte(chunkSize - 20);
                                    ChunkParser.xmlStruct.byteSrc = Utils.replaceBytes(ChunkParser.xmlStruct.byteSrc, modifyByteSize, chunkSizeStart);

                                    // 删除属性内容
                                    int delStart = data.offset;
                                    int delSize = data.getLen();
                                    ChunkParser.xmlStruct.byteSrc = Utils.removeByte(ChunkParser.xmlStruct.byteSrc, delStart, delSize);

                                    modifyFileSize();
                                    return;

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 更改属性值
     *
     * @param tag       标签名
     * @param attrName  属性名称
     * @param attrValue 新值
     */
    public static void modifyAttr(String tag, String attrName, String attrValue) {

        ChunkParser.parserXml();
        AXmlEditor.removeAttr(tag, attrName);
        ChunkParser.parserXml();
        AXmlEditor.addAttr(tag, attrName, attrValue);

    }

    /**
     * 添加属性值
     *
     * @param tag       标签
     * @param attrName  新属性名
     * @param attrVal 新属性值
     */
    public static void addAttr(String tag, String attrName, String attrVal) {

        ChunkParser.parserXml();
        attrValue = Integer.valueOf(attrVal);
        // 构造一个属性出来
        int[] type = getAttrType(attrVal);
        int attr_name = getStrIndex(attrName);
        int attr_value = getStrIndex(attrVal);
        int attr_uri = getStrIndex(prefixStr);

        int attr_type = type[0]; // 属性类型
        int attr_data = type[1]; // 属性值, 是 int 类型

        AttributeData data = AttributeData.createAttribute(attr_uri, attr_name, attr_value, attr_type, attr_data);

        for (StartTagChunk chunk : ChunkParser.xmlStruct.startTagChunkList) {

            int tagNameIndex = Utils.byte2int(chunk.name);
            String tagNameTmp = ChunkParser.xmlStruct.stringChunk.stringContentList.get(tagNameIndex);
            if (tag.equals(tagNameTmp)) {

                // 如果是 application, manifest 标签直接处理就好
                if (tag.equals("application") || tag.equals("manifest") || tag.equals("uses-sdk")) {
                    // 还得修改对应的 tag chunk 中属性个个数和大小
                    int countStart = chunk.offset + 28;
                    byte[] modifyByte = Utils.int2Byte(chunk.attrList.size() + 1);
                    ChunkParser.xmlStruct.byteSrc = Utils.replaceBytes(ChunkParser.xmlStruct.byteSrc, modifyByte, countStart);

                    // 修改 chunk 的大小
                    int chunkSizeStart = chunk.offset + 4;
                    int chunkSize = Utils.byte2int(chunk.size);
                    byte[] modifyByteSize = Utils.int2Byte(chunkSize + 20);
                    ChunkParser.xmlStruct.byteSrc = Utils.replaceBytes(ChunkParser.xmlStruct.byteSrc, modifyByteSize, chunkSizeStart);

                    // 添加属性内容到原来的 chunk 上
                    ChunkParser.xmlStruct.byteSrc = Utils.insertByte(ChunkParser.xmlStruct.byteSrc, chunk.offset + chunkSize, data.getBytes());

                    modifyStringChunk();

                    modifyFileSize();

                    return;
                }

                for (AttributeData attrData : chunk.attrList) {
                    String attrNameTemp = ChunkParser.xmlStruct.stringChunk.stringContentList.get(attrData.name);
                    if ("name".equals(attrNameTemp)) { // 得先找到 tag 对应的唯一名称

                        // 还得修改对应的 tag chunk 中属性个个数和大小
                        int countStart = chunk.offset + 28;
                        byte[] modifyByte = Utils.int2Byte(chunk.attrList.size() + 1);
                        ChunkParser.xmlStruct.byteSrc = Utils.replaceBytes(ChunkParser.xmlStruct.byteSrc, modifyByte, countStart);

                        // 修改 chunk 的大小
                        int chunkSizeStart = chunk.offset + 4;
                        int chunkSize = Utils.byte2int(chunk.size);
                        byte[] modifyByteSize = Utils.int2Byte(chunkSize + 20);
                        ChunkParser.xmlStruct.byteSrc = Utils.replaceBytes(ChunkParser.xmlStruct.byteSrc, modifyByteSize, chunkSizeStart);

                        // 添加属性内容到原来的 chunk 上
                        ChunkParser.xmlStruct.byteSrc = Utils.insertByte(ChunkParser.xmlStruct.byteSrc, chunk.offset + chunkSize, data.getBytes());

                        modifyStringChunk();

                        modifyFileSize();

                        return;
                    }
                }
            }
        }

    }

    /**
     * 重新插入 String Chunk 内容块
     */
    public static void modifyStringChunk() {

        // 写入 StartTagChunk chunk 之前, 因为有字符串信息增加, 所以得修改字符串内容
        StringChunk strChunk = ChunkParser.xmlStruct.stringChunk;
        byte[] newStrChunkB = strChunk.getByte(ChunkParser.xmlStruct.stringChunk.stringContentList);
        // 删除原始 String Chunk
        ChunkParser.xmlStruct.byteSrc = Utils.removeByte(ChunkParser.xmlStruct.byteSrc, ChunkParser.stringChunkOffset, Utils.byte2int(strChunk.size));
        // 插入新的 String Chunk
        ChunkParser.xmlStruct.byteSrc = Utils.insertByte(ChunkParser.xmlStruct.byteSrc, ChunkParser.stringChunkOffset, newStrChunkB);

    }

    /**
     * 修改文件最终的大小
     */
    public static void modifyFileSize() {

        byte[] newFileSize = Utils.int2Byte(ChunkParser.xmlStruct.byteSrc.length);
        ChunkParser.xmlStruct.byteSrc = Utils.replaceBytes(ChunkParser.xmlStruct.byteSrc, newFileSize, 4);

    }

    /**
     * 获取字符串的索引值, 如果字符串存在直接返回, 不存在就放到末尾返回对应的索引值
     *
     * @param str 目标字符串
     * @return 索引或新建字符串索引
     */
    public static int getStrIndex(String str) {

        if (str == null || str.length() == 0) {
            return -1;
        }
        for (int i = 0; i < ChunkParser.xmlStruct.stringChunk.stringContentList.size(); i++) {
            if (ChunkParser.xmlStruct.stringChunk.stringContentList.get(i).equals(str)) {
                return i;
            }
        }
        ChunkParser.xmlStruct.stringChunk.stringContentList.add(str);
        return ChunkParser.xmlStruct.stringChunk.stringContentList.size() - 1;

    }

    /**
     * 判断是否是 application 外部的标签, application 的内部和外部标签需要区分对待
     *
     * @param tagName 标签名
     * @return 如果不是 application 内部标签, 返回 true
     */
    public static boolean isNotAppTag(String tagName) {

        for (String str : isNotAppTag) {
            if (str.equals(tagName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取属性对应类型值
     *
     * @param tagValue 标签值
     * @return 类型值和数据
     */
    public static int[] getAttrType(String tagValue) {

        int[] result = new int[2];

        if (tagValue.equals(String.valueOf(true)) || tagValue.equals(String.valueOf(false))) { // boolean
            result[0] = AttributeType.ATTR_BOOLEAN;
            if (tagValue.equals(String.valueOf(true))) {
                result[1] = 1;
            } else {
                result[1] = 0;
            }
        } else if (tagValue.equals("singleTask") || tagValue.equals("standard")
                 || tagValue.equals("singleTop") || tagValue.equals("singleInstance")) { // 启动模式 int 类型
            result[0] = AttributeType.ATTR_FIRSTINT;
            switch (tagValue) {
                case "standard":
                    result[1] = 0;
                    break;
                case "singleTop":
                    result[1] = 1;
                    break;
                case "singleTask":
                    result[1] = 2;
                    break;
                default:
                    result[1] = 3;
                    break;
            }
        } else if (tagValue.equals("minSdkVersion")  || tagValue.equals("targetSdkVersion") || tagValue.equals("versionCode")) {
            result[0] = AttributeType.ATTR_FIRSTINT;
            result[1] = attrValue;
        } else if (tagValue.startsWith("@")) { // 引用
            result[0] = AttributeType.ATTR_REFERENCE;
            result[1] = 0x7F000000;
        } else if (tagValue.startsWith("#")) { // 色值
            result[0] = AttributeType.ATTR_ARGB4;
            result[1] = 0xFFFFFFFF;
        } else { // 字符串
            result[0] = result[0] | AttributeType.ATTR_STRING;
            result[1] = getStrIndex(tagValue);
        }

        result[0] = result[0] | 0x08000000;
        result[0] = Utils.byte2int(Utils.reverseBytes(Utils.int2Byte(result[0]))); // 字节需要翻转一次

        return result;
    }

}
