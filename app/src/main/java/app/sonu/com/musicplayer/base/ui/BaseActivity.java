package app.sonu.com.musicplayer.base.ui;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

/**
 * Created by sonu on 29/6/17.
 */

public abstract class BaseActivity<Presenter extends BaseMvpPresenter> extends AppCompatActivity
        implements BaseMvpView {

    @Inject
    protected Presenter mPresenter;

    @Override
    public void onStart() {
        super.onStart();

        // there is a warning because the compiler does not already know if
        // BaseMvpView's child implemented by this class is same as BaseMvpView's
        // child given to Presenter
        mPresenter.onAttach(this);

        mPresenter.onStart();
    }


}