package ki.zq.remfq.util;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Objects;

import ki.zq.remfq.R;

public class LoadingDialog extends Dialog {

    final TextView tvLoading;
    final ImageView ivLoading;

    public LoadingDialog(Context context) {
        this(context, "处理数据中");
    }

    public LoadingDialog(Context context, String string) {
        this(context, R.style.loading_dialog, string);
    }

    protected LoadingDialog(Context context, int theme, String string) {
        super(context, theme);
        setCanceledOnTouchOutside(false);
        setContentView(R.layout.view_loading);
        tvLoading = findViewById(R.id.tv_loading);
        tvLoading.setText(string);
        ivLoading = findViewById(R.id.iv_loading);
        Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(context, R.anim.loading_animation);
        ivLoading.startAnimation(hyperspaceJumpAnimation);
        Objects.requireNonNull(getWindow()).getAttributes().gravity = Gravity.CENTER;//居中显示
        getWindow().getAttributes().dimAmount = 0.5f;//背景透明度 取值范围 0 ~ 1
    }
}