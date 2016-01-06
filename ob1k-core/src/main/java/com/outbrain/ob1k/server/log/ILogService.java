package com.outbrain.ob1k.server.log;

import com.outbrain.ob1k.Service;
import rx.Observable;

/**
 * Created by aronen on 3/1/15.
 */
public interface ILogService extends Service {
    Observable<String> handle();
}
