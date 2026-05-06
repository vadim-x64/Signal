package com.test.myproject.signal.network;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

// Цей клас потрібен для реального відслідковування відправки байтів (Upload Test)
public class ProgressRequestBody extends RequestBody {

    private final RequestBody delegate;
    private final UploadCallbacks listener;

    public interface UploadCallbacks {
        void onProgressUpdate(long bytesWritten, long contentLength);
    }

    public ProgressRequestBody(RequestBody delegate, UploadCallbacks listener) {
        this.delegate = delegate;
        this.listener = listener;
    }

    @Override
    public MediaType contentType() {
        return delegate.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return delegate.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        BufferedSink bufferedSink = Okio.buffer(new CountingSink(sink));
        delegate.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    private class CountingSink extends ForwardingSink {
        private long bytesWritten = 0;
        private long contentLength = 0;

        public CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            if (contentLength == 0) {
                contentLength = contentLength();
            }
            bytesWritten += byteCount;
            listener.onProgressUpdate(bytesWritten, contentLength);
        }
    }
}