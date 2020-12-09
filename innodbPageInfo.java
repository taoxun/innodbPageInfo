package com.youyouxunyin.test.innodb;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class InnodbPageInfo {

    private static String PATH = "D:\\mysql-5.7.26-winx64\\data\\test\\t7.ibd";

    private static int INNODB_PAGE_SIZE = 1024*16; // InnoDB Page 16K
    private static int FIL_PAGE_OFFSET = 4; // page offset inside space
    private static int FIL_PAGE_TYPE = 24; // File page type
    private static int FIL_PAGE_DATA = 38;//Start of the data on the page
    private static int PAGE_LEVEL = 26;//level of the node in an index tree; the leaf level is the level 0 */
    private static int PAGE_N_RECS = 16; //number of user records

    private static Map<String,String> pageTypeMap = new HashMap<>();
    private static Map<String,Integer> pageTypeCount = new HashMap<>();
    private static boolean verbose = false;

    static {
        pageTypeMap.put("0000","Freshly Allocated Page");
        pageTypeMap.put("0002","Undo Log Page");
        pageTypeMap.put("0003","File Segment inode");
        pageTypeMap.put("0004","Insert Buffer Free List");
        pageTypeMap.put("0005","Insert Buffer Bitmap");
        pageTypeMap.put("0006","System Page");
        pageTypeMap.put("0007","Transaction system Page");
        pageTypeMap.put("0008","File Space Header");
        pageTypeMap.put("0009","extend description page");
        pageTypeMap.put("000a","Uncompressed BLOB Page");
        pageTypeMap.put("000b","1st compressed BLOB Page");
        pageTypeMap.put("000c","Subsequent compressed BLOB Page");
        pageTypeMap.put("45bf","B-tree Node");
    }

    public static void cmdLineAnalysis(String[] args){
        Map<String,String> cmdLine = new HashMap<>();
        for (int i=0;i<args.length;i+=2){
            cmdLine.put(args[i],args[i+1]);
        }
        if (cmdLine.containsKey("-path")){
            PATH = cmdLine.get("-path");
        }
        if (cmdLine.containsKey("-v")){
            String v = cmdLine.get("-v");
            verbose = "0".equals(v);
        }
    }


    public static void main(String[] args) throws Exception {
        cmdLineAnalysis(args);
        File file = new File(PATH);
        long length = file.length();
        FileInputStream stream = new FileInputStream(file);
        FileChannel fileChannel = stream.getChannel();
        long pageNumber = length / INNODB_PAGE_SIZE;
        long offset = 0;
        ByteBuffer byteBuffer = ByteBuffer.allocate(INNODB_PAGE_SIZE);
        int records = 0;
        try{
            for (int i=0;i<pageNumber;i++){
                byteBuffer.clear();
                fileChannel.read(byteBuffer,offset);
                String pageOffset = read(byteBuffer,FIL_PAGE_OFFSET,4);
                String pageType = read(byteBuffer, FIL_PAGE_TYPE, 2);
                String pageTypeName = pageTypeMap.get(pageType);
                if (verbose){
                    if ("45bf".equals(pageType)){
                        String pageLevel = read(byteBuffer,FIL_PAGE_DATA + PAGE_LEVEL,2);
                        if (Integer.valueOf(pageLevel,16)==0){
                            String pageRecs = read(byteBuffer,FIL_PAGE_DATA + PAGE_N_RECS,2);
                            records+=Integer.valueOf(pageRecs,16);
                        }
                        String format = String.format("page offset %s,page type <%s>,page level <%s>",pageOffset,pageTypeName,pageLevel);
                        System.out.println(format);
                    }else {
                        String format = String.format("page offset %s,page type <%s>", pageOffset,pageTypeName);
                        System.out.println(format);
                    }
                }
                if (pageTypeCount.containsKey(pageTypeName)){
                    pageTypeCount.put(pageTypeName,pageTypeCount.get(pageTypeName)+1);
                }else {
                    pageTypeCount.put(pageTypeName,1);
                }
                offset+=INNODB_PAGE_SIZE;
            }
            if (verbose){
                System.out.println("数据页总记录数:"+records);
            }
            System.out.println(String.format("Total number of page: %d",pageNumber));
            for(Map.Entry<String, Integer> entry : pageTypeCount.entrySet()){
                String key = entry.getKey();
                Integer value = entry.getValue();
                System.out.println(String.format("%s: %s",key,value));
            }
        }finally {
            if (fileChannel!=null){
                fileChannel.close();
            }
            if (stream!=null){
                stream.close();
            }
        }
    }
    private static String read(ByteBuffer buffer,int offset,int length){
        byte[] bytes = new byte[length];
        buffer.position(offset);
        buffer.get(bytes);
        String hexString = bytesToHexString(bytes);
        return hexString;
    }
    /*
     * 字节数组转16进制字符串
     */
    public static String bytesToHexString(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer(bArr.length);
        String sTmp;
        for (int i = 0; i < bArr.length; i++) {
            sTmp = Integer.toHexString(0xFF & bArr[i]);
            if (sTmp.length() < 2)
                sb.append(0);
            sb.append(sTmp);
        }
        return sb.toString();
    }
}
