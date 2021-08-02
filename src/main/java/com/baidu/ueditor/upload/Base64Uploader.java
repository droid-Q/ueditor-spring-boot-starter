package com.baidu.ueditor.upload;

import com.baidu.ueditor.PathFormat;
import com.baidu.ueditor.define.AppInfo;
import com.baidu.ueditor.define.BaseState;
import com.baidu.ueditor.define.FileType;
import com.baidu.ueditor.define.State;
import com.baidu.ueditor.spring.EditorController;
import org.springframework.util.Base64Utils;

import java.util.Map;

public class Base64Uploader {

    public static State save(String content, Map<String, Object> conf) {
        byte[] data = decode(content);
        long maxSize = (Long) conf.get("maxSize");
        if (!validSize(data, maxSize)) {
            return new BaseState(false, AppInfo.MAX_SIZE);
        }
        String suffix = FileType.getSuffix("JPG");
        String savePath = PathFormat.parse((String) conf.get("savePath"), (String) conf.get("filename"));
        savePath = savePath + suffix;
        String physicalPath = PathFormat.format(EditorController.properties.getLocal().getPhysicalPath() + "/" + savePath);
        State storageState = StorageManager.saveBinaryFile(data, physicalPath);
        if (storageState.isSuccess()) {
            storageState.putInfo("url", PathFormat.format(conf.get("contextPath") + "/" + EditorController.properties.getLocal().getUrlPrefix() + savePath));
            storageState.putInfo("type", suffix);
            storageState.putInfo("original", "");
        }
        return storageState;
    }

    private static byte[] decode(String content) {
        return Base64Utils.decodeFromString(content);
    }

    private static boolean validSize(byte[] data, long length) {
        return data.length <= length;
    }

}