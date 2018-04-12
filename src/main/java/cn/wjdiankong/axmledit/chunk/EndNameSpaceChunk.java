package cn.wjdiankong.axmledit.chunk;

import cn.wjdiankong.axmledit.helper.Utils;

public class EndNameSpaceChunk {

    public byte[] type = new byte[4];
    public byte[] size = new byte[4];
    public byte[] lineNumber = new byte[4];
    public byte[] unknown = new byte[4];
    public byte[] prefix = new byte[4];
    public byte[] uri = new byte[4];

    public static EndNameSpaceChunk createChunk(byte[] byteSrc) {

        EndNameSpaceChunk chunk = new EndNameSpaceChunk();

        // 解析 type
        chunk.type = Utils.copyByte(byteSrc, 0, 4);

        // 解析 size
        chunk.size = Utils.copyByte(byteSrc, 4, 4);

        // 解析行号
        chunk.lineNumber = Utils.copyByte(byteSrc, 8, 4);

        // 解析 unknown
        chunk.unknown = Utils.copyByte(byteSrc, 12, 4);

        // 解析 prefix (这里需要注意的是行号后面的四个字节为 0xFFFF, 过滤)
        chunk.prefix = Utils.copyByte(byteSrc, 16, 4);

        // 解析 Uri
        chunk.uri = Utils.copyByte(byteSrc, 20, 4);

        return chunk;

    }

}
