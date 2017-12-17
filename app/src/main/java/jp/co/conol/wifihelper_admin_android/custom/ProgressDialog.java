package jp.co.conol.wifihelper_admin_android.custom;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.constraint.ConstraintLayout;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.TextView;

import jp.co.conol.wifihelper_admin_android.R;

/**
 * Created by m_ito on 2017/12/06.
 */

public class ProgressDialog {

    private boolean isShowing = false;    // プログレスダイアログが表示されているか否か
    private ConstraintLayout mProgressDialogLayout;     // 読み込みダイアログ全体
    private TextView mProgressDialogTextView;   // 読み込みダイアログのメッセージ
    private boolean mCanceledOnTouchOutside = false; // 領域外をタップでダイアログを閉じるか否か
    private final int FADE_IN_TIME = 200;  // ダイアログのフェードイン速度（ミリ秒）
    private final int FADE_OUT_TIME = 200;  // ダイアログのフェードアウト速度（ミリ秒）
    private final int RADIUS = 8;         // ダイアログの角丸の大きさ

    public ProgressDialog(Activity activity) {
        mProgressDialogLayout = (ConstraintLayout) activity.findViewById(R.id.progressDialogLayout);
        mProgressDialogTextView = (TextView) activity.findViewById(R.id.progressDialogTextView);
        ConstraintLayout outSideConstraintLayoutStart = (ConstraintLayout) activity.findViewById(R.id.outSideConstraintLayoutStart);
        ConstraintLayout outSideConstraintLayoutEnd = (ConstraintLayout) activity.findViewById(R.id.outSideConstraintLayoutEnd);
        ConstraintLayout outSideConstraintLayoutTop = (ConstraintLayout) activity.findViewById(R.id.outSideConstraintLayoutTop);
        ConstraintLayout outSideConstraintLayoutBottom = (ConstraintLayout) activity.findViewById(R.id.outSideConstraintLayoutBottom);
        ConstraintLayout dialogConstraintLayout = (ConstraintLayout) activity.findViewById(R.id.dialogConstraintLayout);

        // ダイアログのbackgroundを設定
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(Color.WHITE);
        gradientDrawable.setCornerRadius(RADIUS);
        dialogConstraintLayout.setBackground(gradientDrawable);

        // 領域外をタップでダイアログを閉じる
        View.OnClickListener dismissClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mCanceledOnTouchOutside) dismiss();
            }
        };
        outSideConstraintLayoutStart.setOnClickListener(dismissClickListener);
        outSideConstraintLayoutEnd.setOnClickListener(dismissClickListener);
        outSideConstraintLayoutTop.setOnClickListener(dismissClickListener);
        outSideConstraintLayoutBottom.setOnClickListener(dismissClickListener);
    }

    public void show() {
        if(!isShowing) {
            isShowing = true;
            mProgressDialogLayout.setVisibility(View.VISIBLE);
            AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setDuration(FADE_IN_TIME);
            mProgressDialogLayout.startAnimation(fadeIn);
            enableTouchBackground(mProgressDialogLayout, false);
        }
    }

    public void dismiss() {
        if(isShowing) {
            isShowing = false;
            final AnimationSet animationSet = new AnimationSet(true);
            AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setDuration(FADE_OUT_TIME);
            animationSet.addAnimation(fadeOut);
            animationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    enableTouchBackground(mProgressDialogLayout, true);
                    mProgressDialogLayout.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            mProgressDialogLayout.startAnimation(animationSet);
        }
    }

    public void setMessage(String message) {
        if(message != null) mProgressDialogTextView.setText(message);
    }

    public void setCanceledOnTouchOutside(boolean canceledOnTouchOutside) {
        mCanceledOnTouchOutside = canceledOnTouchOutside;
    }

    public boolean isShowing() {
        return isShowing;
    }

    private void enableTouchBackground(View view, final boolean state) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return !state;
            }
        });
    }
}
