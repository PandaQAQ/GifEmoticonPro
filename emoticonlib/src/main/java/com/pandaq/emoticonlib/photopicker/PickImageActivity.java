package com.pandaq.emoticonlib.photopicker;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pandaq.emoticonlib.EmoticonManager;
import com.pandaq.emoticonlib.R;
import com.pandaq.emoticonlib.base.SwipeBackActivity;
import com.pandaq.emoticonlib.utils.Constant;
import com.pandaq.emoticonlib.utils.EmoticonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by huxinyu on 2017/11/9 0010.
 * description ：
 */

public class PickImageActivity extends SwipeBackActivity implements AdapterView.OnItemClickListener, View.OnClickListener {

    private LineGridView mGvPictures;

    private Map<String, ArrayList<String>> picMap = new HashMap<>();
    private CheckPicAdapter mPicAdapter;
    private BottomSheetDialog mBottomSheetDialog;
    private ArrayList<ImageFileBean> mImageBeen;
    private final int ACTION_TAKE_PHOTO = 20;
    private static final String takePhotoPath = "images/user_take.jpg";
    private static final String defaultStickerPath = EmoticonManager.getInstance().getStickerPath() + "/selfSticker";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_photo);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        TextView tvSelectAlbum = (TextView) findViewById(R.id.tv_bottom_left);
        TextView tvBottomRight = (TextView) findViewById(R.id.tv_bottom_right);
        TextView tvActionManage = (TextView) findViewById(R.id.tv_action_manage);
        RelativeLayout rlBottomLayout = (RelativeLayout) findViewById(R.id.rl_bottom_layout);
        mGvPictures = (LineGridView) findViewById(R.id.gv_pictures);
        View toolbarSplit = findViewById(R.id.toolbar_split);

        tvBottomRight.setVisibility(View.GONE);
        rlBottomLayout.setVisibility(View.VISIBLE);
        tvActionManage.setVisibility(View.GONE);
        toolbarSplit.setVisibility(View.GONE);
        toolbar.setTitle("选择图片");

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickImageActivity.this.finish();
            }
        });
        mImageBeen = new ArrayList<>();
        mGvPictures.setOnItemClickListener(this);
        tvSelectAlbum.setOnClickListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestRunTimePermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, new PermissionCall() {
                @Override
                public void requestSuccess() {
                    initImages();
                }

                @Override
                public void refused() {
                    Toast.makeText(PickImageActivity.this, "请授予必要权限！！", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            initImages();
        }
    }

    private void initImages() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "未发现存储设备！", Toast.LENGTH_SHORT).show();
        }
        Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = this.getContentResolver();
        Cursor cursor = contentResolver.query(imageUri, null, MediaStore.Images.Media.MIME_TYPE + "=? or "
                        + MediaStore.Images.Media.MIME_TYPE + "=?"
                , new String[]{"image/jpeg", "image/jpg"}, MediaStore.Images.Media.DATE_MODIFIED);
        if (cursor == null) {
            Toast.makeText(this, "未发现存储设备！", Toast.LENGTH_SHORT).show();
            return;
        }
        while (cursor.moveToNext()) {
            //图片路径名
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            //图片父路径名
            String parentPath = new File(path).getParentFile().getName();
            if (!picMap.containsKey(parentPath)) {
                ArrayList<String> childList = new ArrayList<>();
                childList.add(path);
                picMap.put(parentPath, childList);
            } else {
                picMap.get(parentPath).add(path);
            }
        }
        cursor.close();
        ArrayList<String> allPath = new ArrayList<>();
        for (Map.Entry<String, ArrayList<String>> entry : picMap.entrySet()) {
            ImageFileBean imageFileBean = new ImageFileBean();
            imageFileBean.setFileName(entry.getKey());
            imageFileBean.setImages(entry.getValue());
            imageFileBean.setTopImage(entry.getValue().get(0));
            mImageBeen.add(imageFileBean);
            allPath.addAll(entry.getValue());
        }
        allPath.add(0, "ic_action_camera");
        ImageFileBean all = new ImageFileBean();
        all.setFileName(getString(R.string.all_pictures));
        if (allPath.size() > 1) {
            all.setTopImage(allPath.get(1)); //去掉相机图片
        }
        all.setImages(allPath);
        mImageBeen.add(0, all);
        showPics(allPath);
        initBottomDialog();
    }

    private void showPics(ArrayList<String> value) {
        if (mPicAdapter == null) {
            mPicAdapter = new CheckPicAdapter(this, value);
            mGvPictures.setAdapter(mPicAdapter);
        } else {
            mPicAdapter.setPicPaths(value);
        }
    }

    private void initBottomDialog() {
        mBottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_bottom_sheet, null, false);
        mBottomSheetDialog.setContentView(view);
        RecyclerView recyclerView = (RecyclerView) mBottomSheetDialog.findViewById(R.id.rv_album_list);
        assert recyclerView != null;
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setSmoothScrollbarEnabled(true);
        recyclerView.setLayoutManager(layoutManager);
        AlbumAdapter adapter = new AlbumAdapter(mImageBeen, this);
        adapter.setItemClickListener(new AlbumAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(ArrayList<String> images) {
                showPics(images);
                mBottomSheetDialog.dismiss();
            }
        });
        recyclerView.setAdapter(adapter);
        setBehaviorCallback();
    }

    private void setBehaviorCallback() {
        View view = mBottomSheetDialog.getDelegate().findViewById(android.support.design.R.id.design_bottom_sheet);
        assert view != null;
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(view);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    mBottomSheetDialog.dismiss();
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String imagePath = mPicAdapter.getItem(position);
        if (imagePath.equals("ic_action_camera")) {
            takePhoto();
        } else {
            Intent intent = new Intent(this, StickerAddPreviewActivity.class);
            intent.putExtra(Constant.SOURCE_PATH, imagePath);
            intent.putExtra(Constant.TARGET_PATH, defaultStickerPath);
            startActivityForResult(intent, 110);
            this.finish();
        }
    }


    /**
     * 拍照
     */
    private void takePhoto() {
        try {
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            File file = new File(EmoticonUtils.getAppFile(this, "images"));
            File mPhotoFile = new File(EmoticonUtils.getAppFile(this, takePhotoPath));
            if (!file.exists()) {
                boolean result = file.mkdirs();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(this, getApplicationInfo().packageName + ".fileprovider", mPhotoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
            } else {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mPhotoFile));
            }
            startActivityForResult(intent, ACTION_TAKE_PHOTO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        File mPhotoFile = new File(EmoticonUtils.getAppFile(this, takePhotoPath));
        switch (requestCode) {
            case ACTION_TAKE_PHOTO:
                if (mPhotoFile.exists()) {
                    Intent intent = new Intent(this, StickerAddPreviewActivity.class);
                    intent.putExtra(Constant.SOURCE_PATH, mPhotoFile.getAbsolutePath());
                    intent.putExtra(Constant.TARGET_PATH, defaultStickerPath);
                    startActivityForResult(intent, 110);
                    this.finish();
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if (mBottomSheetDialog == null) {
            return;
        }
        if (mBottomSheetDialog.isShowing()) {
            mBottomSheetDialog.dismiss();
        } else {
            mBottomSheetDialog.show();
        }
    }
}
