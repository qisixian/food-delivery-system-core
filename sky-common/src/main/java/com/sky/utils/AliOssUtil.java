package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            log.error("Caught an OSSException, which means your request made it to OSS, but was rejected with an error response for some reason. " +
                    "Error Message: {}. Error Code: {}. Request ID: {}. Host ID: {}.", oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
            // TODO 报错应该直接返回错误消息或者抛出异常，但是这里为了暂时不实现OSS相关功能继续开发，先注释掉
            // return null;
        } catch (ClientException ce) {
            log.error("Caught an ClientException, which means the client encountered a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network. Error Message: {}" , ce.getMessage());
            // TODO 报错应该直接返回错误消息或者抛出异常，但是这里为了暂时不实现OSS相关功能继续开发，先注释掉
            // return null;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        log.info("文件上传到:{}", stringBuilder.toString());

        return stringBuilder.toString();
    }
}
