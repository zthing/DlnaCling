package com.zt.test.dlnacling;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SpanUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.kk.dlnacling.DlnaClingObserver;
import com.kk.dlnacling.UpnpUtil;
import com.zt.test.R;

import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.support.avtransport.callback.GetTransportInfo;
import org.fourthline.cling.support.avtransport.callback.Pause;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private DlnaClingObserver dlnaClingObserver;
    private ArrayList<Device> devices;
    private DeviceAdapter deviceAdapter;
    private TestUrlAdapter urlAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dlnacling_activity_main);
        RecyclerView rv_device = findViewById(R.id.rv_device);
        RecyclerView rv_url = findViewById(R.id.rv_url);
        rv_device.setItemAnimator(null);
        rv_device.setNestedScrollingEnabled(false);
        rv_url.setItemAnimator(null);
        rv_url.setNestedScrollingEnabled(false);

        urlAdapter = new TestUrlAdapter();
        rv_url.setAdapter(urlAdapter);
        urlAdapter.setOnItemClickListener((adapter, view, position) -> {
            urlAdapter.setSelection(position);
            if (dlnaClingObserver.getSelectionDevice() != null) {
                play();
            }
        });
        dlnaClingObserver = new DlnaClingObserver(this, new DefaultRegistryListener() {
            @Override
            public void deviceAdded(Registry registry, final Device device) {
                runOnUiThread(() -> {
                    devices.add(device);
                    deviceAdapter.notifyItemInserted(devices.size() - 1);
                });
            }

            @Override
            public void deviceRemoved(Registry registry, final Device device) {
                runOnUiThread(() -> {
                    int position = devices.indexOf(device);
                    devices.remove(device);
                    deviceAdapter.notifyItemRemoved(position);
                });
            }
        }, this);
        devices = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(dlnaClingObserver, devices);
        rv_device.setAdapter(deviceAdapter);
        deviceAdapter.setOnItemClickListener((adapter, view, position) -> {
            int tempPosition = -1;
            Device oldSelectionDevice = dlnaClingObserver.getSelectionDevice();
            if (oldSelectionDevice != null) {
                tempPosition = devices.indexOf(oldSelectionDevice);
            }
            dlnaClingObserver.setSelectionDevice(devices.get(position));
            if (tempPosition != -1) adapter.notifyItemChanged(tempPosition);
            adapter.notifyItemChanged(position);
            play();
        });
    }

    // 投屏
    private void play() {
        String url = urlAdapter.getItem(urlAdapter.getSelection());
        dlnaClingObserver.autoPlay(url, UpnpUtil.VIDEO_TYPE, new DlnaClingObserver.SimpleExecuteCallback() {
            @Override
            public void callback(boolean success) {
                ToastUtils.showLong("投屏" + (success ? "成功" : "失败"));
            }
        });
    }

    @Override
    public void onClick(View v) {
        final Service transportService = dlnaClingObserver.getTransportService();
        switch (v.getId()) {
            case R.id.btn_playPause: // 播放/暂停
                if (transportService == null) break;
                dlnaClingObserver.execute(new GetTransportInfo(transportService) {
                    @Override
                    public void received(ActionInvocation actionInvocation, TransportInfo transportInfo) {
                        if (transportInfo.getCurrentTransportState() == TransportState.PLAYING) {
                            dlnaClingObserver.execute(new Pause(transportService) {
                                @Override
                                public void success(ActionInvocation invocation) {
                                    ToastUtils.showLong("暂停成功");
                                }

                                @Override
                                public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                                    ToastUtils.showLong("暂停失败：" + s);
                                }
                            });
                        } else {
                            dlnaClingObserver.execute(new Play(transportService) {
                                @Override
                                public void success(ActionInvocation invocation) {
                                    ToastUtils.showLong("播放成功");
                                }

                                @Override
                                public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                                    ToastUtils.showLong("播放失败：" + s);
                                }
                            });

                        }
                    }

                    @Override
                    public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                        ToastUtils.showLong("操作失败：" + s);
                    }
                });
                break;
            case R.id.btn_volumeSub:
                dlnaClingObserver.setVolume(-1, new DlnaClingObserver.SimpleExecuteCallback() {
                    @Override
                    public void callback(boolean success) {
                        ToastUtils.showLong("降低音量" + (success ? "成功" : "失败"));
                    }
                });
                break;
            case R.id.btn_volumeAdd:
                dlnaClingObserver.setVolume(1, new DlnaClingObserver.SimpleExecuteCallback() {
                    @Override
                    public void callback(boolean success) {
                        ToastUtils.showLong("音量增加" + (success ? "成功" : "失败"));
                    }
                });
                break;
            case R.id.btn_seekSub5:
                dlnaClingObserver.seek(-5, new DlnaClingObserver.SimpleExecuteCallback() {
                    @Override
                    public void callback(boolean success) {
                        ToastUtils.showLong("快退" + (success ? "成功" : "失败"));
                    }
                });
                break;
            case R.id.btn_seekAdd5:
                dlnaClingObserver.seek(5, new DlnaClingObserver.SimpleExecuteCallback() {
                    @Override
                    public void callback(boolean success) {
                        ToastUtils.showLong("快进" + (success ? "成功" : "失败"));
                    }
                });
                break;
            case R.id.btn_stop:
                if (transportService == null) break;
                dlnaClingObserver.execute(new Stop(transportService) {
                    @Override
                    public void success(ActionInvocation invocation) {
                        ToastUtils.showLong("停止成功");
                    }

                    @Override
                    public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String s) {
                        ToastUtils.showLong("停止失败：" + s);
                    }
                });
                break;
        }
    }

    private static class DeviceAdapter extends BaseQuickAdapter<Device, BaseViewHolder> {

        private final DlnaClingObserver dlnaClingObserver;

        private DeviceAdapter(@NonNull DlnaClingObserver dlnaClingObserver, @Nullable List<Device> data) {
            super(android.R.layout.simple_list_item_1, data);
            this.dlnaClingObserver = dlnaClingObserver;
//            View emptyView = LayoutInflater.from(mContext).inflate(android.R.layout.simple_list_item_1, null);
//            emptyView.<TextView>findViewById(android.R.id.text1).setText("暂未搜索到设备");
//            setEmptyView(emptyView);
        }

        @Override
        protected void convert(@NonNull BaseViewHolder helper, Device item) {
            SpanUtils.with(helper.<TextView>getView(android.R.id.text1))
                    .append(item.getDetails().getFriendlyName())
                    .append("  [ " + (item.getDetails().getBaseURL() == null ? "null" : item.getDetails().getBaseURL().getHost()) + " ] ")
                    .setFontSize(10, true)
                    .setForegroundColor(Color.GRAY)
                    .create();
            boolean isSelection = item == dlnaClingObserver.getSelectionDevice();
            helper.setTextColor(android.R.id.text1, isSelection ? Color.BLACK : Color.GRAY)
                    .setBackgroundColor(android.R.id.text1, isSelection ? Color.parseColor("#DCDCDC") : Color.TRANSPARENT)
                    .<TextView>getView(android.R.id.text1).setGravity(Gravity.CENTER_VERTICAL);
        }
    }

    private static class TestUrlAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
        private static final List<String> urls = Arrays.asList(
                "http://ivi.bupt.edu.cn/hls/cctv1hd.m3u8",
                "http://player.alicdn.com/video/aliyunmedia.mp4",
                "http://video19.ifeng.com/video07/2013/11/11/281708-102-007-1138.mp4",
                "http://t.live.pull.kuaikuaikeji.com/kk/live_kkhd_z.m3u8?auth_key=1582293882-0-0-d80555f75701d583992236af1c5971c4",
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/kkvideo/b3c68c1c-f376-4107-bdd5-ecce563d692a.mp4"
        );

        private TestUrlAdapter() {
            super(android.R.layout.simple_list_item_1, urls);
        }

        private int selection = 0;

        private int getSelection() {
            return selection;
        }

        private void setSelection(int selection) {
            int tempPosition = this.selection;
            this.selection = selection;
            notifyItemChanged(tempPosition);
            notifyItemChanged(selection);
        }

        @Override
        protected void convert(@NonNull BaseViewHolder helper, String item) {
            helper.setText(android.R.id.text1, item)
                    .setTextColor(android.R.id.text1, selection == helper.getLayoutPosition() ? Color.BLACK : Color.GRAY)
                    .setBackgroundColor(android.R.id.text1, selection == helper.getLayoutPosition() ? Color.parseColor("#DCDCDC") : Color.TRANSPARENT);
        }
    }
}
