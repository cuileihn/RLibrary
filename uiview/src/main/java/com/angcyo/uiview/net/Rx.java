package com.angcyo.uiview.net;

import android.text.TextUtils;

import com.angcyo.library.utils.L;
import com.angcyo.uiview.github.type.TypeBuilder;
import com.angcyo.uiview.utils.Json;
import com.angcyo.uiview.utils.T;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by robi on 2016-04-21 15:41.
 */
public class Rx<Rx> extends Observable<Rx> {

    /**
     * 网络请求错误, 重试的次数
     */
    public static final long RETRY_COUNT = 0;
    public static final Observable.Transformer<T, T> ioSchedulersTransformer = new Observable.Transformer<T, T>() {
        @Override
        public Observable<T> call(Observable<T> tObservable) {
            return tObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        }
    };
    public static final Observable.Transformer<T, T> newThreadSchedulersTransformer = new Observable.Transformer<T, T>() {
        @Override
        public Observable<T> call(Observable<T> tObservable) {
            return tObservable.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());
        }
    };

    protected Rx(OnSubscribe<Rx> f) {
        super(f);
    }

    public static final <T> Observable.Transformer<T, T> applyNewThreadSchedulers() {
        return (Observable.Transformer<T, T>) newThreadSchedulersTransformer;
    }

    public static final <T> Observable.Transformer<T, T> applyIOSchedulers() {
        return (Observable.Transformer<T, T>) ioSchedulersTransformer;
    }

    public static <T> Observable.Transformer<T, T> normalSchedulers() {
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> source) {
                return source.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    public static <T, R> Subscription base(T t, Func1<? super T, ? extends R> func) {
        return Observable.just(t).map(func).compose(applyIOSchedulers()).subscribe();
    }

    /**
     * <pre>
     *
     * Rx.base(object : RFunc<HomeBean>() {
     *      override fun call(t: String?): HomeBean? {
     *      return null
     *  }
     * }, object : RSubscriber<HomeBean>() {
     *
     * })
     *
     * </pre>
     * 简单的子线程,转主线程调用
     */
    public static <R> Subscription base(RFunc<? extends R> onBack, RSubscriber<R> onMain) {
        return Observable
                .just("base")
                .map(onBack)
                .compose(new Transformer<R, R>() {
                    @Override
                    public Observable<R> call(Observable<R> rObservable) {
                        return rObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
                    }
                })
                .subscribe(onMain);
    }

    public static <T, R> Subscription base(T t, Func1<? super T, ? extends R> func, Scheduler scheduler) {
        return scheduler == Schedulers.newThread() ?
                Observable.just(t).map(func).compose(applyNewThreadSchedulers()).subscribe()
                :
                Observable.just(t).map(func).compose(applyIOSchedulers()).subscribe();
    }

    public static <R> Subscription base(Func1<String, ? extends R> func) {
        return base("-", func);
    }

    public static <R> Subscription base(Func1<String, ? extends R> func, Scheduler scheduler) {
        return base("-", func, scheduler);
    }

    public static <T> Observable.Transformer<T, T> transformer() {
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> tObservable) {
                return tObservable.unsubscribeOn(Schedulers.io())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    /**
     * 不检查数据返回, 直接转换成Bean
     */
    public static <T> Observable.Transformer<ResponseBody, T> transformerBean(final Class<T> type) {
        return new Observable.Transformer<ResponseBody, T>() {

            @Override
            public Observable<T> call(Observable<ResponseBody> responseObservable) {
                return responseObservable.unsubscribeOn(Schedulers.io())
                        .subscribeOn(Schedulers.io())
                        .map(new Func1<ResponseBody, T>() {
                            @Override
                            public T call(ResponseBody stringResponse) {
                                T bean;
                                String body;
                                try {
                                    body = stringResponse.string();

                                    //"接口返回数据-->\n" +
                                    L.json(body);

                                    bean = Json.from(body, type);
                                    return bean;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    //throw new RException(-1000, "服务器数据异常.", e.getMessage());
                                }
                                //throw new NullPointerException("无数据.");
                                return null;
                            }
                        })
                        .retry(RETRY_COUNT)
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    public static <T> Observable.Transformer<ResponseBody, T> transformer(final Class<T> type) {
        return new Observable.Transformer<ResponseBody, T>() {

            @Override
            public Observable<T> call(Observable<ResponseBody> responseObservable) {
                return responseObservable.unsubscribeOn(Schedulers.io())
                        .subscribeOn(Schedulers.io())
                        .map(new Func1<ResponseBody, T>() {
                            @Override
                            public T call(ResponseBody stringResponse) {
                                T bean;
                                String body;
                                try {
                                    body = stringResponse.string();

                                    //"接口返回数据-->\n" +
                                    L.json(body);

                                    JSONObject jsonObject = new JSONObject(body);
                                    int result = 0;
                                    try {
                                        result = jsonObject.getInt("result");
                                    } catch (Exception e) {
                                        //兼容资讯API
                                        result = jsonObject.getInt("code");
                                        if (result == 200) {
                                            result = 1;//资讯接口没有此字段
                                        }
                                    }

                                    if (result == 1) {
                                        //请求成功
                                        String data = jsonObject.getString("data");
                                        if (!TextUtils.isEmpty(data)) {
                                            if (type == String.class) {
                                                bean = (T) data;
                                                return bean;
                                            }
                                            bean = Json.from(data, type);
                                            return bean;
                                        }
                                    } else {
                                        //请求成功, 但是有错误
                                        JSONObject errorObject = jsonObject.getJSONObject("error");

                                        throw new RException(errorObject.getInt("code"),
                                                errorObject.getString("msg"),
                                                errorObject.getString("more"));
                                    }
                                } catch (JSONException | IOException e) {
                                    e.printStackTrace();
                                    //throw new RException(-1000, "服务器数据异常.", e.getMessage());
                                }
                                //throw new NullPointerException("无数据.");
                                return null;
                            }
                        })
                        .retry(RETRY_COUNT)
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    public static <T> Observable.Transformer<ResponseBody, T> transformRedPacket(final Class<T> type) {
        return new Observable.Transformer<ResponseBody, T>() {

            @Override
            public Observable<T> call(Observable<ResponseBody> responseObservable) {
                return responseObservable.unsubscribeOn(Schedulers.io())
                        .subscribeOn(Schedulers.io())
                        .map(new Func1<ResponseBody, T>() {
                            @Override
                            public T call(ResponseBody stringResponse) {
                                T bean;
                                String body;
                                try {
                                    body = stringResponse.string();

                                    //"接口返回数据-->\n" +
                                    L.json(body);

                                    JSONObject jsonObject = new JSONObject(body);
                                    int result = -1;
                                    try {
                                        result = jsonObject.getInt("code");
                                    } catch (Exception e) {

                                    }

                                    if (result == 0) {
                                        //请求成功
                                        String data = jsonObject.getString("data");
                                        if (!TextUtils.isEmpty(data)) {
                                            bean = Json.from(data, type);
                                            return bean;
                                        }
                                    } else {
                                        //请求成功, 但是有错误
                                        JSONObject errorObject = jsonObject.getJSONObject("error");

                                        throw new RException(errorObject.getInt("code"),
                                                errorObject.getString("msg"),
                                                errorObject.getString("more"));
                                    }
                                } catch (JSONException | IOException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        })
                        .retry(RETRY_COUNT)
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    public static <T> Observable.Transformer<ResponseBody, List<T>> transformerList(final Class<T> type) {
        return new Observable.Transformer<ResponseBody, List<T>>() {

            @Override
            public Observable<List<T>> call(Observable<ResponseBody> responseObservable) {
                return responseObservable.unsubscribeOn(Schedulers.io())
                        .subscribeOn(Schedulers.io())
                        .map(new Func1<ResponseBody, List<T>>() {
                            @Override
                            public List<T> call(ResponseBody stringResponse) {
                                List<T> bean = new ArrayList<>();
                                String body;
                                try {
                                    body = stringResponse.string();

                                    //"接口返回数据-->\n" +
                                    L.json(body);

                                    JSONObject jsonObject = new JSONObject(body);
                                    int result = 0;
                                    try {
                                        result = jsonObject.getInt("result");
                                    } catch (Exception e) {
                                        ////兼容资讯API
                                        result = jsonObject.getInt("code");
                                        if (result == 200) {
                                            result = 1;//资讯接口没有此字段
                                        }
                                    }
                                    if (result == 1) {
                                        //请求成功
                                        String data = jsonObject.getString("data");
                                        if (!TextUtils.isEmpty(data)) {
                                            bean = Json.from(data, TypeBuilder.newInstance(List.class).addTypeParam(type).build());
                                            return bean;
                                        }
                                    } else {
                                        //请求成功, 但是有错误
                                        JSONObject errorObject = jsonObject.getJSONObject("error");

                                        throw new RException(errorObject.getInt("code"),
                                                errorObject.getString("msg"),
                                                errorObject.getString("more"));
                                    }
                                } catch (JSONException | IOException e) {
                                    e.printStackTrace();
                                }
                                return bean;
                            }
                        })
                        .retry(RETRY_COUNT)
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }
}
