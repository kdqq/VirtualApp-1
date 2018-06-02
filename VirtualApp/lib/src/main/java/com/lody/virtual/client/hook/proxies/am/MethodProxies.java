package com.lody.virtual.client.hook.proxies.am;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Application;
import android.app.IServiceConnection;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.webkit.MimeTypeMap;

import com.lody.virtual.client.NativeEngine;
import com.lody.virtual.client.VClient;
import com.lody.virtual.client.badger.BadgerManager;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.hook.delegate.TaskDescriptionDelegate;
import com.lody.virtual.client.hook.providers.ProviderHook;
import com.lody.virtual.client.hook.secondary.ServiceConnectionDelegate;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.client.ipc.ActivityClientRecord;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VAppPermissionManager;
import com.lody.virtual.client.ipc.VNotificationManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.stub.ChooserActivity;
import com.lody.virtual.client.stub.InstallerActivity;
import com.lody.virtual.client.stub.ShadowPendingActivity;
import com.lody.virtual.client.stub.UnInstallerActivity;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.compat.ActivityManagerCompat;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.compat.UriCompat;
import com.lody.virtual.helper.utils.ArrayUtils;
import com.lody.virtual.helper.utils.BitmapUtils;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.DrawableUtils;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.remote.AppTaskInfo;
import com.lody.virtual.server.interfaces.IAppRequestListener;
import com.xdja.zs.controllerManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.WeakHashMap;

import mirror.android.app.IActivityManager;
import mirror.android.app.LoadedApk;
import mirror.android.content.ContentProviderHolderOreo;
import mirror.android.content.IIntentReceiverJB;
import mirror.android.content.pm.UserInfo;

/**
 * @author Lody
 */
@SuppressWarnings("unused")
class MethodProxies {


    static class ForceStopPackage extends MethodProxy {

        @Override
        public String getMethodName() {
            return "forceStopPackage";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String pkg = (String) args[0];
            int userId = VUserHandle.myUserId();
            VActivityManager.get().killAppByPkg(pkg, userId);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class CrashApplication extends MethodProxy {

        @Override
        public String getMethodName() {
            return "crashApplication";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class AddPackageDependency extends MethodProxy {

        @Override
        public String getMethodName() {
            return "addPackageDependency";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class GetPackageForToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPackageForToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            String pkg = VActivityManager.get().getPackageForToken(token);
            if (pkg != null) {
                return pkg;
            }
            return super.call(who, method, args);
        }
    }

    static class UnbindService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unbindService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IServiceConnection conn = (IServiceConnection) args[0];
            ServiceConnectionDelegate delegate = ServiceConnectionDelegate.removeDelegate(conn);
            if (delegate == null) {
                return method.invoke(who, args);
            }
            return VActivityManager.get().unbindService(delegate);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    static class GetContentProviderExternal extends GetContentProvider {

        @Override
        public String getMethodName() {
            return "getContentProviderExternal";
        }

        @Override
        public int getProviderNameIndex() {
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class StartVoiceActivity extends StartActivity {
        @Override
        public String getMethodName() {
            return "startVoiceActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }


    static class UnstableProviderDied extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unstableProviderDied";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args[0] == null) {
                return 0;
            }
            return method.invoke(who, args);
        }
    }


    static class PeekService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "peekService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceLastAppPkg(args);
            Intent service = (Intent) args[0];
            String resolvedType = (String) args[1];
            return VActivityManager.get().peekService(service, resolvedType);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetPackageAskScreenCompat extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPackageAskScreenCompat";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (args.length > 0 && args[0] instanceof String) {
                    args[0] = getHostPkg();
                }
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetIntentSender extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getIntentSender";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String creator = (String) args[1];
            String[] resolvedTypes = (String[]) args[6];
            int indexOfFirst = ArrayUtils.indexOfFirst(args, IBinder.class);
            IBinder iBinder = indexOfFirst == -1 ? null : (IBinder) args[indexOfFirst];
            int type = (int) args[0];
            int flags = (int) args[7];
            if (args[5] instanceof Intent[]) {
                Intent[] intents = (Intent[]) args[5];
                for (int i = 0; i < intents.length; i++) {
                    Intent intent = intents[i];
                    if (resolvedTypes != null && i < resolvedTypes.length) {
                        intent.setDataAndType(intent.getData(), resolvedTypes[i]);
                    }
                    Intent targetIntent = ComponentUtils.redirectIntentSender(type, creator, intent, iBinder);
                    if (targetIntent != null) {
                        intents[i] = targetIntent;
                    }
                }
            }
            args[7] = flags;
            args[1] = getHostPkg();
            // Force userId to 0
            if (args[args.length - 1] instanceof Integer) {
                args[args.length - 1] = 0;
            }
            IInterface sender = (IInterface) method.invoke(who, args);
            if (sender != null && creator != null) {
                VActivityManager.get().addPendingIntent(sender.asBinder(), creator);
            }
            return sender;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    static class StartActivity extends MethodProxy {

        private static final String SCHEME_FILE = "file";
        private static final String SCHEME_PACKAGE = "package";
        private static final String SCHEME_CONTENT = "content";

        @Override
        public String getMethodName() {
            return "startActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if(!controllerManager.getActivitySwitch()){
                return 0;
            }
            int intentIndex = ArrayUtils.indexOfObject(args, Intent.class, 1);
            if (intentIndex < 0) {
                return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
            }
            int resultToIndex = ArrayUtils.indexOfObject(args, IBinder.class, 2);
            String resolvedType = (String) args[intentIndex + 1];
            Intent intent = (Intent) args[intentIndex];

            Log.e("lxf","startActivity intent "+ intent.toString());
            String action = intent.getAction();
            if(Intent.ACTION_VIEW.equals(action)&&"*/*".equals(resolvedType)){
                String suffix = MimeTypeMap.getFileExtensionFromUrl(intent.getDataString());
                Log.e("lxf","startActivity suffix "+ suffix);
                if(suffix!=null){
                    String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
                    if(type!=null)
                        resolvedType = type;
                    Log.e("lxf","startActivity resolvedType "+ resolvedType);
                }
            }

            Log.e("lxf","startActivity action "+ action);
            Log.e("lxf","startActivity uri "+ intent.getDataString());

            intent.setDataAndType(intent.getData(), resolvedType);
            IBinder resultTo = resultToIndex >= 0 ? (IBinder) args[resultToIndex] : null;
            int userId = VUserHandle.myUserId();

            if ("android.intent.action.MAIN".equals(intent.getAction())
                    && intent.hasCategory("android.intent.category.HOME")) {
                return method.invoke(who, args);
            }

            if (ComponentUtils.isStubComponent(intent)) {
                Log.e("lxf","startActivity isStubComponent "+ true);
                Log.e("lxf","startActivity isStubComponent "+ intent.getComponent().getPackageName());

                return method.invoke(who, args);
            }

            //权限管控
            VAppPermissionManager vAppPermissionManager = VAppPermissionManager.get();
            //禁止使用照相机
            boolean cameraEnable = vAppPermissionManager.getAppPermissionEnable(getAppPkg()
                    , VAppPermissionManager.PROHIBIT_CAMERA);
            Log.e("geyao", getAppPkg() + " Camera Enable: " + cameraEnable);
            if (cameraEnable && MediaStore.ACTION_IMAGE_CAPTURE.equals(intent.getAction())) {
                vAppPermissionManager.interceptorTriggerCallback(getAppPkg(), VAppPermissionManager.PROHIBIT_CAMERA);
                return 0;
            }
            //禁止对此应用进行截屏,录屏
            boolean appPermissionEnable = vAppPermissionManager.getAppPermissionEnable(getAppPkg()
                    , VAppPermissionManager.PROHIBIT_SCREEN_SHORT_RECORDER);
            ComponentName component = intent.getComponent();
            Log.e("geyao", "component packageName: " + (component == null ? "component is null" : component.getPackageName()));
            Log.e("geyao", "component className: " + (component == null ? "component is null" : component.getClassName()));
            Log.e("geyao", "component permissionEnable: " + appPermissionEnable);
            if (component != null && "com.android.systemui".equals(component.getPackageName())
                    && "com.android.systemui.media.MediaProjectionPermissionActivity".equals(component.getClassName())
                    && appPermissionEnable) {
                vAppPermissionManager.interceptorTriggerCallback(getAppPkg(), VAppPermissionManager.PROHIBIT_SCREEN_SHORT_RECORDER);
                return 0;
            }

            //xdja swbg
//            if(getAppPkg() != null && getAppPkg().equals("com.xdja.swbg")
//                    &&Intent.ACTION_VIEW.equals(intent.getAction())
//                    &&Intent.FLAG_ACTIVITY_NEW_TASK==intent.getFlags()
//                    &&intent.getType()!=null&&intent.getType().equals("*/*")){
//                Log.d("StartActivity", "lxf "+"this is New Task.");
//
//                boolean hasWps = false;
//                List<PackageInfo> listInfos = VPackageManager.get().getInstalledPackages(0, VUserHandle.myUserId());
//                for (PackageInfo info : listInfos){
//                    if(info.packageName.equals("cn.wps.moffice_eng")){
//                        hasWps = true;
//                        break;
//                    }
//                }
//
//                Log.d("StartActivity", "lxf hasWps "+hasWps);
//                if(hasWps){
//                    intent.setClassName("cn.wps.moffice_eng",
//                            "cn.wps.moffice.documentmanager.PreStartActivity");
//                }else{
//                    CharSequence tips = "Not Have WPS!";
//                    android.widget.Toast toast = Toast.makeText.call(getHostContext(), R.string.noApplications,Toast.LENGTH_SHORT);
//
//                    Log.d("StartActivity", "lxf toast "+toast);
//                    toast.show();
//                    return 0;
//                }
//            }
            //xdja

            if (Intent.ACTION_INSTALL_PACKAGE.equals(intent.getAction())
                    || (Intent.ACTION_VIEW.equals(intent.getAction())
                    && "application/vnd.android.package-archive".equals(intent.getType()))) {
                /*if (handleInstallRequest(intent)) {
                    return 0;
                }*/
                intent.putExtra("source_apk", VirtualRuntime.getInitialPackageName());
                intent.putExtra("installer_path", parseInstallRequest(intent));
                intent.setComponent(new ComponentName(getHostContext(), InstallerActivity.class));
                intent.setData(null);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                return method.invoke(who, args);
            } else if ((Intent.ACTION_UNINSTALL_PACKAGE.equals(intent.getAction())
                    || Intent.ACTION_DELETE.equals(intent.getAction()))
                    && "package".equals(intent.getScheme())) {
                /*if (handleUninstallRequest(intent)) {
                    return 0;
                }*/
                String pkg = "";
                Uri packageUri = intent.getData();
                if (SCHEME_PACKAGE.equals(packageUri.getScheme())) {
                    pkg = packageUri.getSchemeSpecificPart();
                }
                intent.putExtra("uninstall_app",pkg);
                intent.setComponent(new ComponentName(getHostContext(), UnInstallerActivity.class));
                intent.setData(null);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                return method.invoke(who, args);
            }

            String resultWho = null;
            int requestCode = -1;
            Bundle options = ArrayUtils.getFirst(args, Bundle.class);
            if (resultTo != null) {
                resultWho = (String) args[resultToIndex + 1];
                requestCode = (int) args[resultToIndex + 2];
            }
            // chooser
            if (ChooserActivity.check(intent)) {
                intent.setComponent(new ComponentName(getHostContext(), ChooserActivity.class));
                intent.putExtra(Constants.EXTRA_USER_HANDLE, userId);
                intent.putExtra(ChooserActivity.EXTRA_DATA, options);
                intent.putExtra(ChooserActivity.EXTRA_WHO, resultWho);
                intent.putExtra(ChooserActivity.EXTRA_REQUEST_CODE, requestCode);
                mirror.android.content.Intent.putExtra.call(intent, ChooserActivity.EXTRA_RESULTTO, resultTo);
                return method.invoke(who, args);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                args[intentIndex - 1] = getHostPkg();
            }
            if (intent.getScheme() != null && intent.getScheme().equals(SCHEME_PACKAGE) && intent.getData() != null) {
                if (intent.getAction() != null && intent.getAction().startsWith("android.settings.")) {
                    intent.setData(Uri.parse("package:" + getHostPkg()));
                }
            }

            ActivityInfo activityInfo = VirtualCore.get().resolveActivityInfo(intent, userId);
            if (activityInfo == null) {
                VLog.e("VActivityManager", "Unable to resolve activityInfo : %s", intent);
                if (intent.getPackage() != null && isAppPkg(intent.getPackage())) {
                    return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
                }
                //TODO: fake file uri
                args[intentIndex] = UriCompat.fakeFileUri(intent);
                return method.invoke(who, args);
            }
            UriCompat.fakeFileUri(intent);
            int res = VActivityManager.get().startActivity(intent, activityInfo, resultTo, options, resultWho, requestCode, VUserHandle.myUserId());
            if (res != 0 && resultTo != null && requestCode > 0) {
                VActivityManager.get().sendActivityResult(resultTo, resultWho, requestCode);
            }
            if (resultTo != null) {
                ActivityClientRecord r = VActivityManager.get().getActivityRecord(resultTo);
                if (r != null && r.activity != null) {
                    try {
                        TypedValue out = new TypedValue();
                        Resources.Theme theme = r.activity.getResources().newTheme();
                        theme.applyStyle(activityInfo.getThemeResource(), true);
                        if (theme.resolveAttribute(android.R.attr.windowAnimationStyle, out, true)) {

                            TypedArray array = theme.obtainStyledAttributes(out.data,
                                    new int[]{
                                            android.R.attr.activityOpenEnterAnimation,
                                            android.R.attr.activityOpenExitAnimation
                                    });

                            r.activity.overridePendingTransition(array.getResourceId(0, 0), array.getResourceId(1, 0));
                            array.recycle();
                        }
                    } catch (Throwable e) {
                        // Ignore
                    }
                }
            }
            return res;
        }

        private String parseInstallRequest(Intent intent){
            Uri packageUri = intent.getData();
            String path = null;
            if (SCHEME_FILE.equals(packageUri.getScheme())) {

                File sourceFile = new File(packageUri.getPath());
                path = NativeEngine.getRedirectedPath(sourceFile.getAbsolutePath());
                VLog.e("wxd", " parseInstallRequest path : " + path);
            }else if (SCHEME_CONTENT.equals(packageUri.getScheme())) {
                InputStream inputStream = null;
                OutputStream outputStream = null;
                File sharedFileCopy = new File(getHostContext().getCacheDir(), packageUri.getLastPathSegment());
                try {
                    inputStream = getHostContext().getContentResolver().openInputStream(packageUri);
                    outputStream = new FileOutputStream(sharedFileCopy);
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, count);
                    }
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    FileUtils.closeQuietly(inputStream);
                    FileUtils.closeQuietly(outputStream);
                }
                path = sharedFileCopy.getPath();
                VLog.e("wxd", " parseInstallRequest sharedFileCopy path : " + path);
            }
            return path;
        }
        private boolean handleInstallRequest(Intent intent) {
            IAppRequestListener listener = VirtualCore.get().getAppRequestListener();
            if (listener != null) {
                Uri packageUri = intent.getData();
                if (SCHEME_FILE.equals(packageUri.getScheme())) {
                    File sourceFile = new File(packageUri.getPath());
                    String path = NativeEngine.getRedirectedPath(sourceFile.getAbsolutePath());
                    try {
                        listener.onRequestInstall(path);
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else if (SCHEME_CONTENT.equals(packageUri.getScheme())) {
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    File sharedFileCopy = new File(getHostContext().getCacheDir(), packageUri.getLastPathSegment());
                    try {
                        inputStream = getHostContext().getContentResolver().openInputStream(packageUri);
                        outputStream = new FileOutputStream(sharedFileCopy);
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, count);
                        }
                        outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        FileUtils.closeQuietly(inputStream);
                        FileUtils.closeQuietly(outputStream);
                    }
                    try {
                        listener.onRequestInstall(sharedFileCopy.getPath());
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        private boolean handleUninstallRequest(Intent intent) {
            IAppRequestListener listener = VirtualCore.get().getAppRequestListener();
            if (listener != null) {
                Uri packageUri = intent.getData();
                if (SCHEME_PACKAGE.equals(packageUri.getScheme())) {
                    String pkg = packageUri.getSchemeSpecificPart();
                    try {
                        listener.onRequestUninstall(pkg);
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
            return false;
        }

    }

    static class StartActivities extends MethodProxy {

        @Override
        public String getMethodName() {
            return "startActivities";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Intent[] intents = ArrayUtils.getFirst(args, Intent[].class);
            String[] resolvedTypes = ArrayUtils.getFirst(args, String[].class);
            IBinder token = null;
            int tokenIndex = ArrayUtils.indexOfObject(args, IBinder.class, 2);
            if (tokenIndex != -1) {
                token = (IBinder) args[tokenIndex];
            }
            Bundle options = ArrayUtils.getFirst(args, Bundle.class);
            return VActivityManager.get().startActivities(intents, resolvedTypes, token, options, VUserHandle.myUserId());
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class FinishActivity extends MethodProxy {
        @Override
        public String getMethodName() {
            return "finishActivity";
        }

        @Override
        public boolean beforeCall(Object who, Method method, Object... args) {
            for (Object o:args) {
                if (o instanceof Intent) {
                    Intent intent = (Intent)o;
                    Uri u = intent.getData();
                    if (u!=null)
                    {
                        Uri newurl = UriCompat.fakeFileUri(u);
                        if (newurl != null) {
                            intent.setDataAndType(newurl, intent.getType());
                        }
                    }
                }
            }
            return super.beforeCall(who, method, args);
        }

        @Override
        public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
            IBinder token = (IBinder) args[0];
            ActivityClientRecord r = VActivityManager.get().getActivityRecord(token);
            boolean taskRemoved = VActivityManager.get().onActivityDestroy(token);
            if (!taskRemoved && r != null && r.activity != null && r.info.getThemeResource() != 0) {
                try {
                    TypedValue out = new TypedValue();
                    Resources.Theme theme = r.activity.getResources().newTheme();
                    theme.applyStyle(r.info.getThemeResource(), true);
                    if (theme.resolveAttribute(android.R.attr.windowAnimationStyle, out, true)) {

                        TypedArray array = theme.obtainStyledAttributes(out.data,
                                new int[]{
                                        android.R.attr.activityCloseEnterAnimation,
                                        android.R.attr.activityCloseExitAnimation
                                });
                        r.activity.overridePendingTransition(array.getResourceId(0, 0), array.getResourceId(1, 0));
                        array.recycle();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            return super.afterCall(who, method, args, result);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetCallingPackage extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCallingPackage";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            return VActivityManager.get().getCallingPackage(token);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetPackageForIntentSender extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getPackageForIntentSender";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface sender = (IInterface) args[0];
            if (sender != null) {
                String packageName = VActivityManager.get().getPackageForIntentSender(sender.asBinder());
                if (packageName != null) {
                    return packageName;
                }
            }
            return super.call(who, method, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    @SuppressWarnings("unchecked")
    static class PublishContentProviders extends MethodProxy {

        @Override
        public String getMethodName() {
            return "publishContentProviders";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetServices extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getServices";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int maxNum = (int) args[0];
            int flags = (int) args[1];
            return VActivityManager.get().getServices(maxNum, flags).getList();
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class GrantUriPermissionFromOwner extends MethodProxy {

        @Override
        public String getMethodName() {
            return "grantUriPermissionFromOwner";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
//            for (int i = 0; i < args.length; i++) {
//                Object obj = args[i];
//                if (obj instanceof Uri) {
//                    Uri uri = UriCompat.fakeFileUri((Uri) obj);
//                    if (uri != null) {
//                        args[i] = uri;
//                    }
//                }
//            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class SetServiceForeground extends MethodProxy {

        @Override
        public String getMethodName() {
            return "setServiceForeground";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (VASettings.DISABLE_FOREGROUND_SERVICE) {
                return 0;
            }
            ComponentName component = (ComponentName) args[0];
            IBinder token = (IBinder) args[1];
            int id = (int) args[2];
            Notification notification = (Notification) args[3];
            boolean removeNotification = false;
            if (args[4] instanceof Boolean) {
                removeNotification = (boolean) args[4];
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && args[4] instanceof Integer) {
                int flags = (int) args[4];
                removeNotification = (flags & Service.STOP_FOREGROUND_REMOVE) != 0;
            } else {
                VLog.e(getClass().getSimpleName(), "Unknown flag : " + args[4]);
            }
            if (!VNotificationManager.get().dealNotification(id, notification, getAppPkg())) {
                notification = new Notification();
                notification.icon = getHostContext().getApplicationInfo().icon;
            }
            /**
             * `BaseStatusBar#updateNotification` aosp will use use
             * `new StatusBarIcon(...notification.getSmallIcon()...)`
             *  while in samsung SystemUI.apk ,the corresponding code comes as
             * `new StatusBarIcon(...pkgName,notification.icon...)`
             * the icon comes from `getSmallIcon.getResource`
             * which will throw an exception on :x process thus crash the application
             */
            if (notification != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    (Build.BRAND.equalsIgnoreCase("samsung") || Build.MANUFACTURER.equalsIgnoreCase("samsung")
                        ||Build.BRAND.equalsIgnoreCase("HUAWEI") || Build.MANUFACTURER.equalsIgnoreCase("HUAWEI"))) {
                notification.icon = getHostContext().getApplicationInfo().icon;
                Icon icon = Icon.createWithResource(getHostPkg(), notification.icon);
                Reflect.on(notification).call("setSmallIcon", icon);
            }

            VActivityManager.get().setServiceForeground(component, token, id, notification, removeNotification);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class UpdateDeviceOwner extends MethodProxy {

        @Override
        public String getMethodName() {
            return "updateDeviceOwner";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    static class GetIntentForIntentSender extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getIntentForIntentSender";
        }

        @Override
        public Object afterCall(Object who, Method method, Object[] args, Object result) throws Throwable {
            Intent intent = (Intent) super.afterCall(who, method, args, result);
            if (intent != null && intent.hasExtra("_VA_|_intent_")) {
                return intent.getParcelableExtra("_VA_|_intent_");
            }
            return intent;
        }
    }


    static class UnbindFinished extends MethodProxy {

        @Override
        public String getMethodName() {
            return "unbindFinished";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            Intent service = (Intent) args[1];
            boolean doRebind = (boolean) args[2];
            VActivityManager.get().unbindFinished(token, service, doRebind);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class StartActivityIntentSender extends MethodProxy {
        @Override
        public String getMethodName() {
            return "startActivityIntentSender";
        }

        /*
        public int startActivityIntentSender(IApplicationThread caller, IntentSender intent,
                    Intent fillInIntent, String resolvedType, IBinder resultTo, String resultWho,
                    int requestCode, int flagsMask, int flagsValues, Bundle options)

        public int startActivityIntentSender(IApplicationThread caller, IIntentSender target,
                    IBinder whitelistToken, Intent fillInIntent, String resolvedType, IBinder resultTo,
                    String resultWho, int requestCode, int flagsMask, int flagsValues, Bundle bOptions)
         */
        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (VASettings.NEW_INTENTSENDER) {
                int intentIndex;
                int resultToIndex;
                int resultWhoIndex;
                int optionsIndex;
                int requestCodeIndex;
                if (BuildCompat.isOreo()) {
                    intentIndex = 3;
                    resultToIndex = 5;
                    resultWhoIndex = 6;
                    requestCodeIndex = 7;
                    optionsIndex = 10;
                } else {
                    optionsIndex = 9;
                    intentIndex = 2;
                    resultToIndex = 4;
                    resultWhoIndex = 5;
                    requestCodeIndex = 6;
                }
                Intent intent = (Intent) args[intentIndex];
                if (intent != null) {
                    IBinder resultTo = (IBinder) args[resultToIndex];
                    String resultWho = (String) args[resultWhoIndex];
                    int requestCode = (int) args[requestCodeIndex];
                    Bundle options = (Bundle) args[optionsIndex];

                    intent.putExtra(ShadowPendingActivity.EXTRA_REQUESTCODE, requestCode);
                    intent.putExtra(ShadowPendingActivity.EXTRA_RESULTWHO, resultWho);
                    intent.putExtra(ShadowPendingActivity.EXTRA_OPTIONS, options);
                    mirror.android.content.Intent.putExtra.call(intent, ShadowPendingActivity.EXTRA_RESULTTO, resultTo);
                }
            }
            return super.call(who, method, args);
        }
    }


    static class BindService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "bindService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface caller = (IInterface) args[0];
            IBinder token = (IBinder) args[1];
            Intent service = (Intent) args[2];
            String resolvedType = (String) args[3];
            IServiceConnection conn = (IServiceConnection) args[4];
            int flags = (int) args[5];
            int userId = VUserHandle.myUserId();
            if (isServerProcess()) {
                userId = service.getIntExtra("_VA_|_user_id_", VUserHandle.USER_NULL);
            }
            if (userId == VUserHandle.USER_NULL) {
                return method.invoke(who, args);
            }
            ServiceInfo serviceInfo = VirtualCore.get().resolveServiceInfo(service, userId);
            if (serviceInfo != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    service.setComponent(new ComponentName(serviceInfo.packageName, serviceInfo.name));
                }
                conn = ServiceConnectionDelegate.getDelegate(conn);
                return VActivityManager.get().bindService(caller.asBinder(), token, service, resolvedType,
                        conn, flags, userId);
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }


    static class StartService extends MethodProxy {
        private static final HashSet<String> BLOCK_ACTION_LIST = new HashSet<>();
        private static final HashSet<String> BLOCK_COMPONENT_LIST = new HashSet<>();

        static {
            BLOCK_ACTION_LIST.add("com.google.android.gms.chimera.container.LOG_LOAD_ATTEMPT");
            BLOCK_ACTION_LIST.add("com.android.vending.contentfilters.IContentFiltersService.BIND");
            BLOCK_ACTION_LIST.add("com.google.android.chimera.FileApkManager.DELETE_UNUSED_FILEAPKS");
            BLOCK_COMPONENT_LIST.add("com.google.android.finsky.contentfilter.impl.ContentFiltersService");
            BLOCK_COMPONENT_LIST.add("com.google.android.gsf.update.SystemUpdateService");
        }

        @Override
        public String getMethodName() {
            return "startService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface appThread = (IInterface) args[0];
            Intent service = (Intent) args[1];
            String resolvedType = (String) args[2];
            if (service == null) {
                return null;
            }
//            if (service.getComponent() != null && service.getComponent().getClassName().contains(StubService.class.getName())) {
//                return method.invoke(who, args);
//            }
            if (BLOCK_ACTION_LIST.contains(service.getAction())) {
                return null;
            }

            if (service.getComponent() != null
                    && getHostPkg().equals(service.getComponent().getPackageName())) {
                // for server process
                return method.invoke(who, args);
            }
            int userId = VUserHandle.myUserId();
            if (service.getBooleanExtra("_VA_|_from_inner_", false)) {
                userId = service.getIntExtra("_VA_|_user_id_", userId);
                service = service.getParcelableExtra("_VA_|_intent_");
            } else {
                if (isServerProcess()) {
                    userId = service.getIntExtra("_VA_|_user_id_", VUserHandle.USER_NULL);
                }
            }
            service.setDataAndType(service.getData(), resolvedType);
            ServiceInfo serviceInfo = VirtualCore.get().resolveServiceInfo(service, VUserHandle.myUserId());
            if (serviceInfo != null) {
                if (!BLOCK_COMPONENT_LIST.contains(serviceInfo.name)) {
                    return VActivityManager.get().startService(appThread, service, resolvedType, userId);
                }
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    static class StartActivityAndWait extends StartActivity {
        @Override
        public String getMethodName() {
            return "startActivityAndWait";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }


    static class PublishService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "publishService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            Intent intent = (Intent) args[1];
            IBinder service = (IBinder) args[2];
            VActivityManager.get().publishService(token, intent, service);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    @SuppressWarnings("unchecked")
    static class GetRunningAppProcesses extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getRunningAppProcesses";
        }

        @Override
        public synchronized Object call(Object who, Method method, Object... args) throws Throwable {
            List<ActivityManager.RunningAppProcessInfo> _infoList = (List<ActivityManager.RunningAppProcessInfo>) method
                    .invoke(who, args);
            if (_infoList == null) {
                return null;
            }
            List<ActivityManager.RunningAppProcessInfo> infoList = new ArrayList<>(_infoList);
            for (ActivityManager.RunningAppProcessInfo info : infoList) {
                if (info.uid == VirtualCore.get().myUid()) {
                    if (VActivityManager.get().isAppPid(info.pid)) {
                        List<String> pkgList = VActivityManager.get().getProcessPkgList(info.pid);
                        String processName = VActivityManager.get().getAppProcessName(info.pid);
                        if (processName != null) {
                            info.processName = processName;
                        }
                        info.pkgList = pkgList.toArray(new String[pkgList.size()]);
                        info.uid = VUserHandle.getAppId(VActivityManager.get().getUidByPid(info.pid));
                    }
                }
            }
            return infoList;
        }
    }


    static class SetPackageAskScreenCompat extends MethodProxy {

        @Override
        public String getMethodName() {
            return "setPackageAskScreenCompat";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (args.length > 0 && args[0] instanceof String) {
                    args[0] = getHostPkg();
                }
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetCallingActivity extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCallingActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            return VActivityManager.get().getCallingActivity(token);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetCurrentUser extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCurrentUser";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            try {
                return UserInfo.ctor.newInstance(0, "user", VUserInfo.FLAG_PRIMARY);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    static class KillApplicationProcess extends MethodProxy {

        @Override
        public String getMethodName() {
            return "killApplicationProcess";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (args.length > 1 && args[0] instanceof String && args[1] instanceof Integer) {
                String processName = (String) args[0];
                int uid = (int) args[1];
                VActivityManager.get().killApplicationProcess(processName, uid);
                return 0;
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class StartActivityAsUser extends StartActivity {

        @Override
        public String getMethodName() {
            return "startActivityAsUser";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }


    static class CheckPermission extends MethodProxy {

        @Override
        public String getMethodName() {
            return "checkPermission";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String permission = (String) args[0];
            if (SpecialComponentList.isWhitePermission(permission)) {
                return PackageManager.PERMISSION_GRANTED;
            }
            if (permission.startsWith("com.google")) {
                return PackageManager.PERMISSION_GRANTED;
            }
            args[args.length - 1] = getRealUid();
            return PackageManager.PERMISSION_GRANTED;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    static class StartActivityAsCaller extends StartActivity {

        @Override
        public String getMethodName() {
            return "startActivityAsCaller";
        }
    }


    static class HandleIncomingUser extends MethodProxy {

        @Override
        public String getMethodName() {
            return "handleIncomingUser";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int lastIndex = args.length - 1;
            if (args[lastIndex] instanceof String) {
                args[lastIndex] = getHostPkg();
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }


    @SuppressWarnings("unchecked")
    static class GetTasks extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getTasks";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = (List<ActivityManager.RunningTaskInfo>) method
                    .invoke(who, args);
            for (ActivityManager.RunningTaskInfo info : runningTaskInfos) {
                AppTaskInfo taskInfo = VActivityManager.get().getTaskInfo(info.id);
                if (taskInfo != null) {
                    info.topActivity = taskInfo.topActivity;
                    info.baseActivity = taskInfo.baseActivity;
                }
            }
            return runningTaskInfos;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetPersistedUriPermissions extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPersistedUriPermissions";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class RegisterReceiver extends MethodProxy {
        private static final int IDX_IIntentReceiver = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                ? 2
                : 1;

        private static final int IDX_RequiredPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                ? 4
                : 3;
        private static final int IDX_IntentFilter = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                ? 3
                : 2;

        private WeakHashMap<IBinder, IIntentReceiver> mProxyIIntentReceivers = new WeakHashMap<>();

        @Override
        public String getMethodName() {
            return "registerReceiver";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            args[IDX_RequiredPermission] = null;
            IntentFilter filter = (IntentFilter) args[IDX_IntentFilter];
            SpecialComponentList.protectIntentFilter(filter, getAppPkg());
            if (args.length > IDX_IIntentReceiver && IIntentReceiver.class.isInstance(args[IDX_IIntentReceiver])) {
                final IInterface old = (IInterface) args[IDX_IIntentReceiver];
                if (!IIntentReceiverProxy.class.isInstance(old)) {
                    final IBinder token = old.asBinder();
                    if (token != null) {
                        token.linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                token.unlinkToDeath(this, 0);
                                mProxyIIntentReceivers.remove(token);
                            }
                        }, 0);
                        IIntentReceiver proxyIIntentReceiver = mProxyIIntentReceivers.get(token);
                        if (proxyIIntentReceiver == null) {
                            proxyIIntentReceiver = new IIntentReceiverProxy(old);
                            mProxyIIntentReceivers.put(token, proxyIIntentReceiver);
                        }
                        WeakReference mDispatcher = LoadedApk.ReceiverDispatcher.InnerReceiver.mDispatcher.get(old);
                        if (mDispatcher != null) {
                            LoadedApk.ReceiverDispatcher.mIIntentReceiver.set(mDispatcher.get(), proxyIIntentReceiver);
                            args[IDX_IIntentReceiver] = proxyIIntentReceiver;
                        }
                    }
                }
            }
            return method.invoke(who, args);
        }


        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

        private static class IIntentReceiverProxy extends IIntentReceiver.Stub {

            IInterface mOld;

            IIntentReceiverProxy(IInterface old) {
                this.mOld = old;
            }

            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered,
                                       boolean sticky, int sendingUser) throws RemoteException {
                //解决税信灭屏幕启动Activity
                if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF)){
                    controllerManager.setActivitySwitch(false);
                }else if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
                    controllerManager.setActivitySwitch(true);
                }

                if (!accept(intent)) {
                    return;
                }
                if (intent.hasExtra("_VA_|_intent_")) {
                    intent = intent.getParcelableExtra("_VA_|_intent_");
                }
                SpecialComponentList.unprotectIntent(intent);
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    IIntentReceiverJB.performReceive.call(mOld, intent, resultCode, data, extras, ordered, sticky, sendingUser);
                } else {
                    mirror.android.content.IIntentReceiver.performReceive.call(mOld, intent, resultCode, data, extras, ordered, sticky);
                }
            }

            private boolean accept(Intent intent) {
                int uid = intent.getIntExtra("_VA_|_uid_", -1);
                if (uid != -1) {
                    return VClient.get().getVUid() == uid;
                }
                int userId = intent.getIntExtra("_VA_|_user_id_", -1);
                if (userId == -1 || userId == VUserHandle.myUserId()) {
                    if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                        if (isFakeLocationEnable()) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }

            @SuppressWarnings("unused")
            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered,
                                       boolean sticky) throws RemoteException {
                this.performReceive(intent, resultCode, data, extras, ordered, sticky, 0);
            }

        }
    }


    static class StopService extends MethodProxy {

        @Override
        public String getMethodName() {
            return "stopService";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IInterface caller = (IInterface) args[0];
            Intent intent = (Intent) args[1];
            String resolvedType = (String) args[2];
            intent.setDataAndType(intent.getData(), resolvedType);
            ComponentName componentName = intent.getComponent();
            PackageManager pm = VirtualCore.getPM();
            if (componentName == null) {
                ResolveInfo resolveInfo = pm.resolveService(intent, 0);
                if (resolveInfo != null && resolveInfo.serviceInfo != null) {
                    componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
                }
            }
            if (componentName != null && !getHostPkg().equals(componentName.getPackageName())) {
                return VActivityManager.get().stopService(caller, intent, resolvedType);
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }


    static class GetContentProvider extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getContentProvider";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            int nameIdx = getProviderNameIndex();
            String name = (String) args[nameIdx];
            int userId = VUserHandle.myUserId();
            ProviderInfo info = VPackageManager.get().resolveContentProvider(name, 0, userId);
            if (info != null && info.enabled && isAppPkg(info.packageName)) {
                int targetVPid = VActivityManager.get().initProcess(info.packageName, info.processName, userId);
                if (targetVPid == -1) {
                    return null;
                }
                args[nameIdx] = VASettings.getStubAuthority(targetVPid);
                Object holder = method.invoke(who, args);
                if (holder == null) {
                    return null;
                }
                if (BuildCompat.isOreo()) {
                    IInterface provider = ContentProviderHolderOreo.provider.get(holder);
                    if (provider != null) {
                        provider = VActivityManager.get().acquireProviderClient(userId, info);
                    }
                    ContentProviderHolderOreo.provider.set(holder, provider);
                    ContentProviderHolderOreo.info.set(holder, info);
                } else {
                    IInterface provider = IActivityManager.ContentProviderHolder.provider.get(holder);
                    if (provider != null) {
                        provider = VActivityManager.get().acquireProviderClient(userId, info);
                    }
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                    IActivityManager.ContentProviderHolder.info.set(holder, info);
                }
                return holder;
            }
            if(SpecialComponentList.isDisableOutsideContentProvider(name)){
                return null;
            }else{
                VLog.w("ActivityManger", "getContentProvider:%s", name);
            }
            Object holder = method.invoke(who, args);
            if (holder != null) {
                if (BuildCompat.isOreo()) {
                    IInterface provider = ContentProviderHolderOreo.provider.get(holder);
                    info = ContentProviderHolderOreo.info.get(holder);
                    if (provider != null) {
                        provider = ProviderHook.createProxy(true, info.authority, provider);
                    }
                    ContentProviderHolderOreo.provider.set(holder, provider);
                } else {
                    IInterface provider = IActivityManager.ContentProviderHolder.provider.get(holder);
                    info = IActivityManager.ContentProviderHolder.info.get(holder);
                    if (provider != null) {
                        provider = ProviderHook.createProxy(true, info.authority, provider);
                    }
                    IActivityManager.ContentProviderHolder.provider.set(holder, provider);
                }
                return holder;
            }
            return null;
        }


        public int getProviderNameIndex() {
            return 1;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static class SetTaskDescription extends MethodProxy {
        @Override
        public String getMethodName() {
            return "setTaskDescription";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            ActivityManager.TaskDescription td = (ActivityManager.TaskDescription) args[1];
            String label = td.getLabel();
            Bitmap icon = td.getIcon();

            // If the activity label/icon isn't specified, the application's label/icon is shown instead
            // Android usually does that for us, but in this case we want info about the contained app, not VIrtualApp itself
            if (label == null || icon == null) {
                Application app = VClient.get().getCurrentApplication();
                if (app != null) {
                    try {
                        if (label == null) {
                            label = app.getApplicationInfo().loadLabel(app.getPackageManager()).toString();
                        }
                        if (icon == null) {
                            Drawable drawable = app.getApplicationInfo().loadIcon(app.getPackageManager());
                            if (drawable != null) {
                                icon = DrawableUtils.drawableToBitMap(drawable);
                            }
                        }
                        td = new ActivityManager.TaskDescription(label, icon, td.getPrimaryColor());
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }

            TaskDescriptionDelegate descriptionDelegate = VirtualCore.get().getTaskDescriptionDelegate();
            if (descriptionDelegate != null) {
                td = descriptionDelegate.getTaskDescription(td);
            }

            args[1] = td;
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class StopServiceToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "stopServiceToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            ComponentName componentName = (ComponentName) args[0];
            IBinder token = (IBinder) args[1];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            int startId = (int) args[2];
            if (componentName != null) {
                return VActivityManager.get().stopServiceToken(componentName, token, startId);
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() || isServerProcess();
        }
    }

    static class StartActivityWithConfig extends StartActivity {
        @Override
        public String getMethodName() {
            return "startActivityWithConfig";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return super.call(who, method, args);
        }
    }

    static class StartNextMatchingActivity extends StartActivity {
        @Override
        public String getMethodName() {
            return "startNextMatchingActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            return false;
        }
    }


    static class BroadcastIntent extends MethodProxy {
        private static final HashSet<String> ACTION_BLACK_LIST = new HashSet<>();

        static {
            ACTION_BLACK_LIST.add("com.google.android.gms.walletp2p.phenotype.ACTION_PHENOTYPE_REGISTER");
            ACTION_BLACK_LIST.add("com.facebook.zero.ACTION_ZERO_REFRESH_TOKEN");
            ACTION_BLACK_LIST.add("com.google.android.gms.magictether.SCANNED_DEVICE");
            ACTION_BLACK_LIST.add("com.google.android.vending.verifier.intent.action.VERIFY_INSTALLED_PACKAGES");
        }

        @Override
        public String getMethodName() {
            return "broadcastIntent";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Intent intent = (Intent) args[1];
            String type = (String) args[2];
            intent.setDataAndType(intent.getData(), type);
            if (VirtualCore.get().getComponentDelegate() != null) {
                VirtualCore.get().getComponentDelegate().onSendBroadcast(intent);
            }
            if (ACTION_BLACK_LIST.contains(intent.getAction())) {
                return 0;
            }
            Intent newIntent = handleIntent(intent);
            if (newIntent != null) {
                args[1] = newIntent;
            } else {
                return 0;
            }

            if (args[7] instanceof String || args[7] instanceof String[]) {
                // clear the permission
                args[7] = null;
            }
            return method.invoke(who, args);
        }


        private Intent handleIntent(final Intent intent) {
            final String action = intent.getAction();
            if ("android.intent.action.CREATE_SHORTCUT".equals(action)
                    || "com.android.launcher.action.INSTALL_SHORTCUT".equals(action)) {

                return VASettings.ENABLE_INNER_SHORTCUT ? handleInstallShortcutIntent(intent) : null;

            } else if ("com.android.launcher.action.UNINSTALL_SHORTCUT".equals(action)) {

                handleUninstallShortcutIntent(intent);

            } else if (BadgerManager.handleBadger(intent)) {
                return null;
            } else {
                return ComponentUtils.redirectBroadcastIntent(intent, VUserHandle.myUserId());
            }
            return intent;
        }

        private Intent handleMediaScannerIntent(Intent intent) {
            if (intent == null) {
                return null;
            }
            Uri data = intent.getData();
            if (data == null) {
                return intent;
            }
            String scheme = data.getScheme();
            if (!"file".equalsIgnoreCase(scheme)) {
                return intent;
            }
            String path = data.getPath();
            if (path == null) {
                return intent;
            }
            String newPath = NativeEngine.getRedirectedPath(path);
            File newFile = new File(newPath);
            if (!newFile.exists()) {
                return intent;
            }
            intent.setData(Uri.fromFile(newFile));
            return intent;
        }

        private Intent handleInstallShortcutIntent(Intent intent) {
            Intent shortcut = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            if (shortcut != null) {
                ComponentName component = shortcut.resolveActivity(VirtualCore.getPM());
                if (component != null) {
                    String pkg = component.getPackageName();
                    Intent newShortcutIntent = new Intent();
                    newShortcutIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    newShortcutIntent.setAction(Constants.SHORTCUT_ACTION);
                    newShortcutIntent.setPackage(getHostPkg());
                    newShortcutIntent.putExtra("_VA_|_intent_", shortcut);
                    newShortcutIntent.putExtra("_VA_|_uri_", shortcut.toUri(0));
                    newShortcutIntent.putExtra("_VA_|_user_id_", VUserHandle.myUserId());
                    intent.removeExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, newShortcutIntent);

                    Intent.ShortcutIconResource icon = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                    if (icon != null && !TextUtils.equals(icon.packageName, getHostPkg())) {
                        try {
                            Resources resources = VirtualCore.get().getResources(pkg);
                            int resId = resources.getIdentifier(icon.resourceName, "drawable", pkg);
                            if (resId > 0) {
                                //noinspection deprecation
                                Drawable iconDrawable = resources.getDrawable(resId);
                                Bitmap newIcon = BitmapUtils.drawableToBitmap(iconDrawable);
                                if (newIcon != null) {
                                    intent.removeExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, newIcon);
                                }
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return intent;
        }

        private void handleUninstallShortcutIntent(Intent intent) {
            Intent shortcut = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            if (shortcut != null) {
                ComponentName componentName = shortcut.resolveActivity(getPM());
                if (componentName != null) {
                    Intent newShortcutIntent = new Intent();
                    newShortcutIntent.putExtra("_VA_|_uri_", shortcut.toUri(0));
                    newShortcutIntent.setClassName(getHostPkg(), Constants.SHORTCUT_PROXY_ACTIVITY_NAME);
                    newShortcutIntent.removeExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, newShortcutIntent);
                }
            }
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class GetActivityClassForToken extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getActivityClassForToken";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            return VActivityManager.get().getActivityForToken(token);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class GrantUriPermission extends MethodProxy {

        @Override
        public String getMethodName() {
            return "grantUriPermission";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
            for (int i = 0; i < args.length; i++) {
                Object obj = args[i];
                if (obj instanceof Uri) {
                    Uri uri = UriCompat.fakeFileUri((Uri) obj);
                    if (uri != null) {
                        args[i] = uri;
                    }
                }
            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class CheckGrantUriPermission extends MethodProxy {

        @Override
        public String getMethodName() {
            return "checkGrantUriPermission";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            MethodParameterUtils.replaceFirstAppPkg(args);
//            for (int i = 0; i < args.length; i++) {
//                Object obj = args[i];
//                if (obj instanceof Uri) {
//                    Uri uri = UriCompat.fakeFileUri((Uri) obj);
//                    if (uri != null) {
//                        args[i] = uri;
//                    }
//                }
//            }
            return method.invoke(who, args);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }


    static class ServiceDoneExecuting extends MethodProxy {

        @Override
        public String getMethodName() {
            return "serviceDoneExecuting";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            if (!VActivityManager.get().isVAServiceToken(token)) {
                return method.invoke(who, args);
            }
            int type = (int) args[1];
            int startId = (int) args[2];
            int res = (int) args[3];
            VActivityManager.get().serviceDoneExecuting(token, type, startId, res);
            return 0;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class OverridePendingTransition extends MethodProxy {
        @Override
        public String getMethodName() {
            return "overridePendingTransition";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (!isNotCopyApk()) {
                //apk res
                return 0;
            }
            return super.call(who, method, args);
        }
    }
}
