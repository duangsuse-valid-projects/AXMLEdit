package cn.wjdiankong.axmledit.chunk;

import cn.wjdiankong.axmledit.helper.Utils;

import java.util.ArrayList;

public class ResourceChunk {

    public byte[] type;
    public byte[] size;

    private byte[] ids;
    private ArrayList<Integer> resourceIdList;

    public static ResourceChunk createChunk(byte[] byteSrc, int offset) {

        ResourceChunk chunk = new ResourceChunk();

        chunk.type = Utils.copyByte(byteSrc, offset, 4);

        chunk.size = Utils.copyByte(byteSrc, 4 + offset, 4);
        int chunkSize = Utils.byte2int(chunk.size);

        // 这里需要注意的是 chunkSize 是包含了 chunkTag 和 chunkSize 这两个字节的, 所以需要剔除
        byte[] ids = Utils.copyByte(byteSrc, 8 + offset, chunkSize - 8);

        chunk.resourceIdList = new ArrayList<>(ids.length / 4);
        for (int i = 0; i < ids.length; i += 4) {
            int resId = Utils.byte2int(Utils.copyByte(ids, i, 4));
            chunk.resourceIdList.add(resId);
        }

        chunk.ids = ids;

        return chunk;

    }

    public ArrayList<Integer> getResourceIdList() {

        return this.resourceIdList;

    }

    public byte[] getIds() {

        return this.ids;

    }

}
