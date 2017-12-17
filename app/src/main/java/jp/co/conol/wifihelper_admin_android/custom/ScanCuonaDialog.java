package jp.co.conol.wifihelper_admin_android.custom;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;

import jp.co.conol.wifihelper_admin_lib.cuona.Cuona;
import jp.co.conol.wifihelper_admin_android.R;

/**
 * Created by m_ito on 2017/12/07.
 */

public class ScanCuonaDialog {

    private boolean isShowing = false;    // スキャンダイアログが表示されているか否か
    private boolean hasForegroundDispatch = true;   // スキャンダイアログを表示時のみNFC読み込み待機をするか否か
    private boolean mCanceledOnTouchOutside = true; // 領域外をタップでダイアログを閉じるか否か
    private Handler mScanDialogAutoCloseHandler = new Handler();
    private Integer mCloseTime = null;  // ダイアログを自動で閉じるまでの時間（ミリ秒）
    private Cuona mCuona = null;
    private Activity mActivity;
    private ConstraintLayout mScanBackgroundConstraintLayout;     // CUONAスキャンダイアログの背景
    private ConstraintLayout mScanDialogConstraintLayout;     // CUONAスキャンダイアログ
    private ConstraintLayout mOutOfDialogConstraintLayout;     // CUONAスキャンダイアログの領域外
    private Button mCancelScanButton;     // キャンセルボタン

    public ScanCuonaDialog(Activity activity, Cuona cuona, Integer closeTime, boolean foregroundDispatch) {
        mScanBackgroundConstraintLayout = (ConstraintLayout) activity.findViewById(R.id.scanBackgroundConstraintLayout);
        mScanDialogConstraintLayout = (ConstraintLayout) activity.findViewById(R.id.scanDialogConstraintLayout);
        mOutOfDialogConstraintLayout = (ConstraintLayout) activity.findViewById(R.id.outOfDialogConstraintLayout);
        mCancelScanButton = (Button) activity.findViewById(R.id.cancelScanButton);
        mCuona = cuona;
        mActivity = activity;
        mCloseTime = closeTime;
        hasForegroundDispatch = foregroundDispatch;

        // ダイアログのbackgroundを設定
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(Color.WHITE);
        gradientDrawable.setCornerRadius(8);
        mScanDialogConstraintLayout.setBackground(gradientDrawable);

        // 「キャンセル」ボタンをクリックで閉じる
        mCancelScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelButtonClicked();
            }
        });

        // 領域外をクリックで閉じる
        mOutOfDialogConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isShowing && mCanceledOnTouchOutside)  dismiss();
            }
        });
    }

    public void show() {
        if(!isShowing) {
            isShowing = true;

            // 読み込み待機
            if (hasForegroundDispatch) mCuona.enableForegroundDispatch(mActivity);

            mOutOfDialogConstraintLayout.setVisibility(View.VISIBLE);

            // ダイアログ部分をスライドイン
            mScanDialogConstraintLayout.setVisibility(View.VISIBLE);
            TranslateAnimation slideInFromBottom = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0f,
                    Animation.RELATIVE_TO_PARENT, 0f,
                    Animation.RELATIVE_TO_PARENT, 1f,
                    Animation.RELATIVE_TO_PARENT, 0f);
            slideInFromBottom.setDuration(300);
            mScanDialogConstraintLayout.setAnimation(slideInFromBottom);

            // 背景をフェードイン
            mScanBackgroundConstraintLayout.setVisibility(View.VISIBLE);
            AlphaAnimation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setDuration(300);
            mScanBackgroundConstraintLayout.startAnimation(fadeIn);

            // 背後はタッチできないように設定
            enableTouchBackground(mScanBackgroundConstraintLayout, false);

            // 指定時間後に自動で閉じる
            if (mCloseTime != null) {
                mScanDialogAutoCloseHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isShowing) {
                            dismiss();
                        }
                    }
                }, mCloseTime);
            }
        }
    }

    public void dismiss() {
        if(isShowing) {
            isShowing = false;

            // 読み込み待機を解除
            if (hasForegroundDispatch) mCuona.disableForegroundDispatch(mActivity);

            // 背景をフェードアウト
            final AnimationSet animationSet = new AnimationSet(true);
            AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setDuration(300);
            animationSet.addAnimation(fadeOut);
            animationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mScanBackgroundConstraintLayout.setVisibility(View.GONE);
                    mOutOfDialogConstraintLayout.setVisibility(View.GONE);
                    enableTouchBackground(mScanBackgroundConstraintLayout, true);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            mScanBackgroundConstraintLayout.startAnimation(animationSet);

            // ダイアログ部分をスライドアウト
            final AnimationSet slideOutToBottomAnimationSet = new AnimationSet(true);
            TranslateAnimation slideInFromBottom = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0f,
                    Animation.RELATIVE_TO_PARENT, 0f,
                    Animation.RELATIVE_TO_PARENT, 0f,
                    Animation.RELATIVE_TO_PARENT, 1f);
            slideInFromBottom.setDuration(300);
            slideOutToBottomAnimationSet.addAnimation(slideInFromBottom);
            slideOutToBottomAnimationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mScanDialogConstraintLayout.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            mScanDialogConstraintLayout.startAnimation(slideOutToBottomAnimationSet);
        }
    }

    public boolean isShowing() {
        return isShowing;
    }

    public void onCancelButtonClicked() {
        dismiss();
    }

    public void setCanceledOnTouchOutside(boolean canceledOnTouchOutside) {
        mCanceledOnTouchOutside = canceledOnTouchOutside;
    }

    // キャンセルボタンのテキスト（デフォルトで「キャンセル」）
    public void setCancelButtonText(String text) {
        mCancelScanButton.setText(text);
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
