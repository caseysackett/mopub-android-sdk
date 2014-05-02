package com.mopub.nativeads;

import android.app.Activity;
import android.support.v4.util.LruCache;

import com.mopub.nativeads.test.support.SdkTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

import static com.mopub.nativeads.CacheService.DiskLruCacheGetListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(SdkTestRunner.class)
public class CacheServiceTest {

    private Activity context;
    private String key1;
    private String data1;
    private String getKey;
    private byte[] getBytes;
    private DiskLruCacheGetListener diskCacheGetListener;
    private Semaphore semaphore;

    @Before
    public void setUp() throws Exception {
        context = new Activity();
        key1 = "http://www.mopub.com/";
        data1 = "image_data_1";

        semaphore = new Semaphore(0);
        diskCacheGetListener = mock(DiskLruCacheGetListener.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                CacheServiceTest.this.getKey = (String)args[0];
                CacheServiceTest.this.getBytes = (byte[])args[1];
                semaphore.release();
                return null;
            }
        }).when(diskCacheGetListener).onComplete(anyString(), any(byte[].class));
    }

    @After
    public void tearDown() throws Exception {
        CacheService.clearAndNullCaches();
    }

    @Test
    public void initializeCaches_withValidContext_shouldCreateNewCachesIdempotently() throws Exception {
        assertThat(CacheService.getDiskLruCache()).isNull();

        CacheService.initializeCaches(context);
        DiskLruCache diskLruCache = CacheService.getDiskLruCache();
        assertThat(diskLruCache).isNotNull();
        LruCache<String, byte[]> memoryLruCache = CacheService.getMemoryLruCache();
        assertThat(memoryLruCache).isNotNull();

        CacheService.initializeCaches(context);
        assertThat(diskLruCache).isEqualTo(CacheService.getDiskLruCache());
        assertThat(memoryLruCache).isEqualTo(CacheService.getMemoryLruCache());
    }
    
    @Test
    public void getDiskLruCacheDirectory_shouldReturnValidCacheDirectory() throws Exception {
        File file = CacheService.getDiskCacheDirectory(context);
        String expectedPath = context.getCacheDir().toString() + "/mopub-cache";
        assertThat(file.getAbsolutePath()).isEqualTo(expectedPath);
    }

    @Test
    public void diskLruCacheGet_whenPopulated_shouldReturnValue() throws Exception {
        CacheService.initializeCaches(context);
        CacheService.putToDiskCache(key1, data1.getBytes());
        assertThat(CacheService.getFromDiskCache(key1)).isEqualTo(data1.getBytes());
    }

    @Test
    public void diskLruCacheGet_whenEmpty_shouldReturnNull() throws Exception {
        CacheService.initializeCaches(context);
        verifyCachesAreEmpty();
        assertThat(CacheService.getFromDiskCache(key1)).isNull();
    }

    @Test
    public void diskLruCacheAsyncGet_whenPopulated_shouldReturnValue() throws Exception {
        CacheService.initializeCaches(context);
        verifyCachesAreEmpty();
        CacheService.putToDiskCache(key1, data1.getBytes());
        CacheService.getFromDiskCacheAsync(key1, diskCacheGetListener);
        semaphore.acquire();
        assertThat(getKey).isEqualTo(key1);
        assertThat(getBytes).isEqualTo(data1.getBytes());
    }

    @Test
    public void diskLruCacheAsyncGet_whenEmpty_shouldReturnNull() throws Exception {
        CacheService.initializeCaches(context);
        CacheService.getFromDiskCacheAsync(key1, diskCacheGetListener);
        semaphore.acquire();
        assertThat(getKey).isEqualTo(key1);
        assertThat(getBytes).isNull();
    }

    @Test
    public void diskLruCachePut_withEmptyStringKey_shouldPutCorrectly() throws Exception {
        // this works because an empty string sha1 hashes to a valid key
        CacheService.initializeCaches(context);
        CacheService.putToDiskCache("", data1.getBytes());
        assertThat(CacheService.getFromDiskCache("")).isEqualTo(data1.getBytes());
    }

    @Test
    public void diskLruCachePut_withNullKey_shouldNotPut() throws Exception {
        // null value produces empty string key which is invalid for disk lru cache
        CacheService.initializeCaches(context);
        verifyCachesAreEmpty();
        CacheService.putToDiskCache(null, data1.getBytes());
        verifyCachesAreEmpty();
    }

    @Test
    public void createValidDiskLruCacheKey_withNullValue_shouldReturnEmptyString() throws Exception {
        CacheService.initializeCaches(context);
        assertThat(CacheService.createValidDiskCacheKey(null)).isEqualTo("");
    }

    @Test
    public void diskLruCacheAsyncPut_whenEmpty_shouldReturnNull() throws Exception {
        CacheService.initializeCaches(context);
        CacheService.putToDiskCacheAsync(key1, data1.getBytes());
        Thread.sleep(500);
        assertThat(CacheService.getFromDiskCache(key1)).isEqualTo(data1.getBytes());
    }

    private static InputStream getInputStreamFromString(final String string) {
        return spy(new ByteArrayInputStream(string.getBytes()));
    }

    static void verifyCachesAreEmpty() {
        assertThat(CacheService.getMemoryLruCache()).isNotNull();
        assertThat(CacheService.getMemoryLruCache().size()).isEqualTo(0);
        assertThat(CacheService.getDiskLruCache()).isNotNull();
        assertThat(CacheService.getDiskLruCache().size()).isEqualTo(0);
    }
}
