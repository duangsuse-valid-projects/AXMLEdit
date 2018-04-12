# AndroidManifest AXML 二进制文件修改工具

此为 [AXMLEditor](https://github.com/fourbrother/AXMLEditor) 重构后的版本, 添加了 `dump/build/plugin` 指令, 修正了部分错误

## 用途

针对于特定 __Android APK__ 反编译修改后无法执行回编译操作, 直接进行 __AXML__ 二进制文件修改, 然后只需要二次签名即可. 无需完全进行反编译和回编译操作

## 关于 `xmlpull` 依赖

原版包含了这个依赖以方便添加 __XML tag__, 如果工具抛出 __NoClassDefFound__ 异常, 确认 __xmlpull.jar__ 与工具放在相同目录下

## 已知问题

+ 在 `AndroidManifest.xml` 的 `application` 标签中添加 `android:debuggable="true"` 时看起来有效但依然没有 __FLAG_DEBUGGABLE__

此问题可能是由于原 `AXMLEditor` 没有充分解析并修改 `AXML` 文件部分结构导致的, 但原工具依然能修改 _(如果原文件中有的话)_ debuggable 为 _true_ 并可以生效

我不会尝试修复这个问题(类似的问题也很多), 不过会提供一个增加 __FLAG_DEBUGGABLE__ 的插件

+ 无法修改 `minSdkVersion` 和 `targetSdkVersion`

原版中存在 `minSdkVersion` 和 `targetSdkVersion` 的兼容, 但兼容的实现是错误的 (参考 _AXmlEditor_ 类 _getAttrType_ 方法)

此外, 原版也不提供 `uses-sdk` 标签的修改功能. 如果一定需要修改, 可以尝试寻找/编写插件

+ 什么是 `绝对名称` ?

简短版本: `<a name="foo"></a>`

在更新时原版的代码里, 对应的参数也没有使用, 我在找到的 _XML_ 和 _AXML_ 文档里也没有提到这个

所以在这个版本里我删除了这个参数

## 用法

### 插入属性

`java -jar AXMLEditor.jar attr -i [标签名] [属性名] [属性值] [输入 axml] [输出 axml]`

示例： `java -jar AXMLEditor.jar -attr -i application debuggable true AndroidManifest.xml AndroidManifest_out.xml`

在 _application_ 标签中插入 _android:debuggable="true"_ 属性, 让程序处于可调试状态

### 删除属性

`java -jar AXMLEditor.jar attr -r [标签名] [属性名] [输入 axml] [输出 axml]`

示例： `java -jar AXMLEditor.jar attr -r application allowBackup AndroidManifest.xml AndroidManifest_out.xml`

在 _application_ 标签中删除 _allowBackup_ 属性, 这样此 app 就可以进行沙盒数据备份

### 更改属性

`java -jar AXMLEditor.jar attr -m [标签名] [属性名] [属性值] [输入 axml] [输出 axml]`

示例： `java -jar AXMLEditor.jar -attr -m application debuggable true AndroidManifest.xml AndroidManifest_out.xml`

在 _application_ 标签中修改 _android:debuggable_ 属性为 __true__, 让程序处于可调试状态

### 插入标签

`java -jar AXMLEditor.jar tag -i [需要插入标签内容的 xml 文件] [输入 axml] [输出 axml]`

示例： `java -jar AXMLEditor.jar tag -i inserting.xml AndroidManifest.xml AndroidManifest_out.xml`

因为插入标签时内容比较多, 命令行方式不方便, 直接输入一个需要插入标签内容的 xml 文件即可

### 删除标签

`java -jar AXMLEditor.jar tag -r [标签名] [输入 axml] [输出 axml]`

示例： `java -jar AXMLEditor.jar tag -r activity cn.wjdiankong.demo.MainActivity AndroidManifest.xml AndroidManifest_out.xml`

删除 __android:name="cn.wjdiankong.demo.MainActivity"__ 标签

### 序列化/反序列化 AXML

`java -jar AXMLEditor.jar dump [输入 axml]`

示例： `java -jar AXMLEditor.jar dump AndroidManifest.xml`

将 __AndroidManifest.xml__ 反序列化输出到 __AndroidManifest.xml.text__ 文本

`java -jar AXMLEditor.jar build [输入 serialized object] [输出 axml]`

示例： `java -jar AXMLEditor.jar build AndroidManifest.xml.text`

将 __AndroidManifest.xml.text__ 序列化输出到 __AndroidManifest.xml__

### 执行插件

`java -jar AXMLEditor.jar plugin add_debuggable.class AndroidManifest.xml AndroidManifest_debuggable.xml`

__AXMLEditor__ 将反射加载初始化插件类, 调用其中的 _XmlStruct process(XmlStruct)_ 方法, 将结果序列化输出
