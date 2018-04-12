package cn.wjdiankong.axmledit.chunk;

/**
 * chunks' magic numbers
 * enum {
 *  CHUNK_HEAD  	= 0x00080003,
 *  <p>
 *  CHUNK_STRING	= 0x001c0001,
 *  CHUNK_RESOURCE 	= 0x00080180,
 *  <p>
 *  CHUNK_STARTNS	= 0x00100100,
 *  CHUNK_ENDNS	    = 0x00100101,
 *  CHUNK_STARTTAG	= 0x00100102,
 *  CHUNK_ENDTAG	= 0x00100103,
 *  CHUNK_TEXT	    = 0x00100104,
 * };
 *
 * @author I
 */
public class ChunkTypeNumber {
    @SuppressWarnings("unused")
    public final static int CHUNK_HEAD = 0x00080003;
    @SuppressWarnings("unused")
    public final static int CHUNK_STRING = 0x001c0001;
    public final static int CHUNK_STARTNS = 0x00100100;
    public final static int CHUNK_ENDNS = 0x00100101;
    public final static int CHUNK_STARTTAG = 0x00100102;
    public final static int CHUNK_ENDTAG = 0x00100103;
    @SuppressWarnings("unused")
    public final static int CHUNK_TEXT = 0x00100104;
}
