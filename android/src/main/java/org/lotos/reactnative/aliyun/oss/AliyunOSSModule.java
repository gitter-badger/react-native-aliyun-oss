package org.lotos.reactnative.aliyun.oss;

import android.util.Log;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.net.URI;

/**
 * Created by liubinbin on 8/9/2016.
 */
public class AliyunOSSModule extends ReactContextBaseJavaModule implements OSSCompletedCallback<PutObjectRequest, PutObjectResult>, OSSProgressCallback<PutObjectRequest> {
    protected ReactApplicationContext context;
    private OSS oss;
    private Promise uploadPromise;

    /**
     * @param reactContext
     */
    public AliyunOSSModule(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;
    }

    @Override
    public String getName() {
        return "AliyunOSSModule";
    }


    @ReactMethod
    public void initWithAppKey(String endpoint, String accessKeyId, String accessKeySecret) {
        // 明文设置secret的方式建议只在测试时使用，更多鉴权模式请参考后面的访问控制章节
        OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret);
        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
        conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
        oss = new OSSClient(context, endpoint, credentialProvider, conf);
    }

    @ReactMethod
    public void upload(String bucketName, String objectKey, String uploadFilePath, final Promise promise) {
        uploadPromise = promise;

        try {
            // 构造上传请求
            String filePath = new URI(uploadFilePath).getPath();
            PutObjectRequest put = new PutObjectRequest(bucketName, objectKey, filePath);
            // 异步上传时可以设置进度回调
            put.setProgressCallback(this);

            OSSAsyncTask task = oss.asyncPutObject(put, this);

//        task.cancel(); // 可以取消任务
//        task.waitUntilFinished(); // 可以等待任务完成
        } catch (Exception e) {
            uploadPromise.reject(e);
        }
    }

    @Override
    public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
        Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
    }

    @Override
    public void onSuccess(PutObjectRequest request, PutObjectResult result) {
        Log.d("PutObject", "UploadSuccess");

        Log.d("ETag", result.getETag());
        Log.d("RequestId", result.getRequestId());
        uploadPromise.resolve(result.getServerCallbackReturnBody());
    }

    @Override
    public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
        // 请求异常
        if (clientExcepion != null) {
            // 本地异常如网络异常等
            clientExcepion.printStackTrace();
            uploadPromise.reject(clientExcepion);
            return;
        }
        if (serviceException != null) {
            // 服务异常
            Log.e("ErrorCode", serviceException.getErrorCode());
            Log.e("RequestId", serviceException.getRequestId());
            Log.e("HostId", serviceException.getHostId());
            Log.e("RawMessage", serviceException.getRawMessage());
            uploadPromise.reject(serviceException);
            return;
        }
        uploadPromise.reject(new UnknownError());
    }
}
