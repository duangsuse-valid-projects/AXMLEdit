package cn.wjdiankong.axmledit;

import cn.wjdiankong.axmledit.helper.AXmlEditor;
import cn.wjdiankong.axmledit.helper.ChunkParser;

import java.io.*;

public class Main {

    private static final String CMD_MODIFY_DONE = "修改属性完成";
    private static final String CMD_HELP = "Usage: java -jar AXMLEditor.jar [tag|attr|help|dump|build|plugin] [-i|-r|-m] [标签名|属性名|属性值] [输入文件|输出文件]";
    private static final String CMD_INSERT_DONE = "插入完成";
    private static final String CMD_DELETE_DONE = "删除完成";

    private static final String ERR_BAD_ARGUMENT = "参数有误";
    private static final String ERR_NO_FILE = "输入文件不存在";
    private static final String ERR_NO_INPUT_XML = "插入标签 xml 文件不存在";
    private static final String ERR_XML_PARSING_ERROR = "Xml parsing error:";
    private static final String ERR_FILE_DELETE = "File delete error";

    public static final String VERSION = "1.0";

    public static int BUFSIZE = 1024;

    public static PrintStream default_out = System.out;
    public static PrintStream default_err = System.err;


    /**
     * 命令格式： [目标集合] [操作名]
     * attr 属性集合
     * tag 标签集合
     * dump 输出反序列化的 axml
     * build 序列化 XmlStruct
     * plugin 加载执行 AXMLEditor 插件
     * help 获得帮助信息
     * -i 添加动作
     * -r 删除动作
     * -m 更新动作
     * 属性操作直接输入参数即可, 标签操作需要输入信息
    */

    public static void main(String[] args) {

        if (args.length < 3) {
            System.out.println(ERR_BAD_ARGUMENT);
            System.out.println(CMD_HELP);
            return;
        }

        String input_file = args[args.length - 2];
        String output_file = args[args.length - 1];
        File inputFile = new File(input_file);
        File outputFile = new File(output_file);
        if (!inputFile.exists()) {
            System.out.println(ERR_NO_FILE);
            return;
        }

        // 读文件
        try (FileInputStream fis = new FileInputStream(inputFile); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFSIZE];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            ChunkParser.xmlStruct.byteSrc = bos.toByteArray();
        } catch (Exception e) {
            System.out.println(ERR_XML_PARSING_ERROR + e.toString());
        }

        doCommand(args);

        // 写文件
        if (!outputFile.exists()) {
            if (outputFile.delete()) {
                System.out.println(ERR_FILE_DELETE);
                System.exit(1);
            }
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
            fos.write(ChunkParser.xmlStruct.byteSrc);
            fos.close();
        } catch (Exception ignored) {
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @SuppressWarnings("unused")
    static void runEditCode() {
        // 删除一个 tag, 删除 tag 时必须指定 tag 名称和 name 值, 这样才能唯一确定一个 tag 信息
        // AXmlEditor.removeTag("uses-permission", "android.permission.INTERNET");
        // AXmlEditor.removeTag("activity", ".MainActivity");

        // 删除属性, 必须要指定属性对应的 tag 名称和 name 值, 然后就是属性名称
        // AXmlEditor.removeAttr("activity", ".MainActivity", "name");
        // AXmlEditor.removeAttr("uses-permission", "android.permission.INTERNET", "name");

        // 添加标签, 直接在 xml 中配置即可, 需要注意的是配置信息 :manifest 下面的标签必须在 application 标签的后面
        // AXmlEditor.addTag();

        // 添加属性, 必须指定标签内容
        // AXmlEditor.addAttr("activity", ".MainActivity", "jiangwei", "fourbrother");

        // 更改属性, 这里直接采用先删除，再添加策略完成
        // AXmlEditor.modifyAttr("application", "package", "debuggable", "true");
    }

    private static void println(String s) {
        default_out.println(s);
    }

    private static void printCommandErr(String info) {
        default_err.println(info);
        default_err.println(CMD_HELP);
    }

    private static void printCommandErr() {
        printCommandErr(ERR_BAD_ARGUMENT);
    }

    private static void doCommand(String[] args) {
        if ("tag".equals(args[0])) {
            if (args.length < 2) {
                printCommandErr();
                return;
            }
            // 标签
            if ("-i".equals(args[1])) {
                if (args.length < 3) {
                    printCommandErr();
                    return;
                }
                // 插入操作
                String insertXml = args[2];
                File file = new File(insertXml);
                if (!file.exists()) {
                    printCommandErr(ERR_NO_INPUT_XML);
                    return;
                }
                AXmlEditor.addTag(insertXml);
                println(CMD_INSERT_DONE);
            } else if ("-r".equals(args[1])) {
                if (args.length < 4) {
                    printCommandErr();
                    return;
                }
                // 删除操作
                String tag = args[2];
                String tagName = args[3];
                AXmlEditor.removeTag(tag, tagName);
                println(CMD_DELETE_DONE);
            } else {
                printCommandErr();
            }
        } else if ("attr".equals(args[0])) {
            if (args.length < 2) {
                printCommandErr();
                return;
            }
            // 属性
            if ("-i".equals(args[1])) {
                if (args.length < 6) {
                    printCommandErr();
                    return;
                }
                // 插入属性
                String tag = args[2];
                String tagName = args[3];
                String attr = args[4];
                String value = args[5];
                AXmlEditor.addAttr(tag, tagName, attr, value);
                println(CMD_INSERT_DONE);
            } else if ("-r".equals(args[1])) {
                if (args.length < 5) {
                    printCommandErr();
                    return;
                }
                // 删除属性
                String tag = args[2];
                String tagName = args[3];
                String attr = args[4];
                AXmlEditor.removeAttr(tag, tagName, attr);
                println(CMD_DELETE_DONE);
            } else if ("-m".equals(args[1])) {
                if (args.length < 6) {
                    printCommandErr();
                    return;
                }
                // 修改属性
                String tag = args[2];
                String tagName = args[3];
                String attr = args[4];
                String value = args[5];
                AXmlEditor.modifyAttr(tag, tagName, attr, value);
                println(CMD_MODIFY_DONE);
            } else {
                printCommandErr();
            }
        }
    }

}
