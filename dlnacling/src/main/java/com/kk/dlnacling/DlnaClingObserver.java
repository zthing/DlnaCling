package com.kk.dlnacling;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DlnaClingObserver implements LifecycleObserver {

    private final Context mContext;
    private final ServiceConnection mUpnpServiceConnection;
    @Nullable
    private Device selectionDevice; // 选中的设备

    private final DlnaClingController mController;

    /**
     * 需要自己调用 onCreate、onDestroy 方法
     *
     * @param context          上下文对象，用于服务绑定
     * @param registryListener 设备变化监听
     */
    public DlnaClingObserver(Context context, RegistryListener registryListener) {
        this(context, registryListener, null);
    }

    /**
     * @param context          上下文对象，用于服务绑定
     * @param registryListener 设备变化监听
     * @param owner            fragment，activity 生命周期绑定，如果为空，则需要自己调用 onCreate、onDestroy 方法
     */
    public DlnaClingObserver(Context context, final RegistryListener registryListener, @Nullable LifecycleOwner owner) {
        this.mContext = context;
        if (owner != null) owner.getLifecycle().addObserver(this);
        mController = new DlnaClingController();
        mUpnpServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                if (iBinder instanceof AndroidUpnpService)
                    mController.initService((AndroidUpnpService) iBinder, registryListener);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mController.setUpnpService(null);
            }
        };
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
//        mContext.getApplicationContext().bindService(new Intent(mContext, AndroidUpnpServiceImpl.class), mUpnpServiceConnection, Context.BIND_AUTO_CREATE);
        mContext.getApplicationContext().bindService(new Intent(mContext, BrowserUpnpService.class), mUpnpServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        Service transportService = getTransportService();
        if (transportService == null) return;
        execute(new Stop(transportService) {
            @Override
            public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
            }
        });
        mContext.getApplicationContext().unbindService(mUpnpServiceConnection);
    }

    @Nullable
    public Device getSelectionDevice() {
        return selectionDevice;
    }

    public void setSelectionDevice(@Nullable Device device) {
        this.selectionDevice = device;
    }

    /**
     * 设置片源
     *
     * @param url 片源地址
     */
    public void autoPlay(String url, int itemType, final ExecuteCallback callback) {
        mController.autoPlay(selectionDevice, url, itemType, callback);
    }

    /**
     * 视频进度调节
     *
     * @param seconds  移动秒数，正数：快进；负数：快退
     * @param callback 投屏结果回调
     */
    public void seek(int seconds, final ExecuteCallback callback) {
        mController.seek(selectionDevice, seconds, callback);
    }

    /**
     * 音量调节
     *
     * @param volume   音量调节数，正/负，最小0，最大100
     * @param callback 投屏结果回调
     */
    public void setVolume(final int volume, final DlnaClingObserver.ExecuteCallback callback) {
        mController.setVolume(selectionDevice, volume, callback);
    }

    /**
     * 执行 投屏命令
     *
     * @param callback 投屏命令及回调
     * @see org.fourthline.cling.support.avtransport.callback
     * @see org.fourthline.cling.support.renderingcontrol.callback 音量-get/set
     */
    public void execute(@NotNull ActionCallback callback) {
        mController.execute(callback);
    }

    /**
     * 得到传输服务
     */
    @Nullable
    public Service getTransportService() {
        if (selectionDevice == null) return null;
        return mController.getTransportService(selectionDevice);
    }

    /**
     * 得到控制服务
     */
    @Nullable
    public Service getControlService() {
        if (selectionDevice == null) return null;
        return mController.getControlService(selectionDevice);
    }

    public interface ExecuteCallback {
        void success(ActionInvocation invocation);

        void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg);
    }

    public abstract static class SimpleExecuteCallback implements ExecuteCallback {

        public abstract void callback(boolean success);

        @Override
        public void success(ActionInvocation invocation) {
            callback(true);
        }

        @Override
        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
            callback(false);
        }
    }
}
