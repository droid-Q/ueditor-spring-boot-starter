package com.baidu.ueditor.spring.support.qiniu;

import com.baidu.ueditor.PathFormat;
import com.baidu.ueditor.define.AppInfo;
import com.baidu.ueditor.define.BaseState;
import com.baidu.ueditor.define.MultiState;
import com.baidu.ueditor.define.State;
import com.baidu.ueditor.spring.EditorUploader;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 七牛云上传实现
 *
 * @author lhy
 * @since 2019-05-06
 */
@Component
public class QiniuUploader implements EditorUploader {

    @Override
    public State binaryUpload(HttpServletRequest request, Map<String, Object> conf) {
        String fieldName = (String) conf.get("fieldName");
        MultiValueMap<String, MultipartFile> multiFileMap = ((MultipartHttpServletRequest) request).getMultiFileMap();
        MultipartFile file = multiFileMap.getFirst(fieldName);
        assert file != null;
        // 文件名
        String originFileName = file.getOriginalFilename();
        assert originFileName != null;
        // 文件扩展名
        String suffix = originFileName.substring(originFileName.lastIndexOf(".")).toLowerCase();
        // 不符合文件类型
        if (!Arrays.asList((String[]) conf.get("allowFiles")).contains(suffix)) {
            return new BaseState(false, AppInfo.NOT_ALLOW_FILE_TYPE);
        }
        long maxSize = (Long) conf.get("maxSize");
        // 文件大小超出限制
        if (maxSize < file.getSize()) {
            return new BaseState(false, AppInfo.MAX_SIZE);
        }
        // 根据config.json 中的 imagePathFormat 生成 路径+文件名
        String savePath = (String) conf.get("savePath");
        savePath = savePath + suffix;
        savePath = PathFormat.parse(savePath, originFileName);
        // TODO: 实现你自己的上传 比如ftp上传到服务器、上传到七牛云等
        String url = QiniuUtils.upload(file, savePath);
        BaseState baseState = new BaseState();
        // 必填项url，图片地址
        baseState.putInfo("url", url);
        return baseState;
    }

    @Override
    public State base64Upload(HttpServletRequest request, Map<String, Object> conf) {
        String filedName = (String) conf.get("fieldName");
        String content = request.getParameter(filedName);
        byte[] data = Base64.decodeBase64(content);
        long maxSize = (Long) conf.get("maxSize");
        // 文件大小超出限制
        if (data.length > maxSize) {
            return new BaseState(false, AppInfo.MAX_SIZE);
        }
        // 根据config.json 中的 imagePathFormat 生成 路径+文件名
        String savePath = PathFormat.parse((String) conf.get("savePath"), (String) conf.get("filename"));
        savePath = savePath + ".jpg";
        // TODO: 实现你自己的上传 比如ftp上传到服务器、上传到七牛云等
        String url = QiniuUtils.upload(content, savePath);
        BaseState baseState = new BaseState();
        // 必填项url，图片地址
        baseState.putInfo("url", url);
        return baseState;
    }


    @Override
    public MultiState listImage(int index, Map<String, Object> conf) {
        return this.listFile(index, conf);
    }

    @Override
    public MultiState listFile(int index, Map<String, Object> conf) {
        // 每次列出图片数量 config.json 中的 imageManagerListSize
        int count = (Integer) conf.get("count");
        String dir = (String) conf.get("dir");
        Total total = new Total();
        List<String> listFile = QiniuUtils.listFile(dir, index, count, total);
        MultiState state = null;
        if (listFile == null) {
            state = new MultiState(true);
        } else {
            state = new MultiState(true);
            BaseState fileState = null;
            for (String key : listFile) {
                fileState = new BaseState(true);
                fileState.putInfo("url", key);
                state.addState(fileState);

            }
        }
        state.putInfo("start", index);
        state.putInfo("total", total.getTotal());
        return state;
    }
}
