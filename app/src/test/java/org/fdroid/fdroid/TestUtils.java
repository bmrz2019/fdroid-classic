package org.fdroid.fdroid;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContextWrapper;
import android.content.pm.ProviderInfo;

import androidx.test.core.app.ApplicationProvider;

import org.mockito.AdditionalAnswers;
import org.robolectric.Robolectric;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class TestUtils {
    public static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }

    public static File copyResourceToTempFile(String resourceName) {
        File tempFile = null;
        InputStream input = null;
        OutputStream output = null;
        try {
            tempFile = File.createTempFile(resourceName + "-", ".testasset");
            input = TestUtils.class.getClassLoader().getResourceAsStream(resourceName);
            output = new FileOutputStream(tempFile);
            Utils.copy(input, output);
        } catch (IOException e) {
            e.printStackTrace();
            if (tempFile != null && tempFile.exists()) {
                assertTrue(tempFile.delete());
            }
            fail();
            return null;
        } finally {
            Utils.closeQuietly(output);
            Utils.closeQuietly(input);
        }
        return tempFile;

    }

    /**
     * The way that Robolectric has to implement shadows for Android classes
     * such as {@link android.content.ContentProvider} is by using a special
     * annotation that means the classes will implement the correct methods at
     * runtime.  However this means that the shadow of a content provider does
     * not actually extend {@link android.content.ContentProvider}. As such,
     * we need to do some special mocking using Mockito in order to provide a
     * {@link ContextWrapper} which is able to return a proper content
     * resolver that delegates to the Robolectric shadow object.
     */
    public static ContextWrapper createContextWithContentResolver(ContentResolver contentResolver) {
        final ContentResolver resolver = mock(ContentResolver.class, AdditionalAnswers.delegatesTo(contentResolver));
        return new ContextWrapper(ApplicationProvider.getApplicationContext()) {
            @Override
            public ContentResolver getContentResolver() {
                return resolver;
            }
        };
    }

    public static <T extends ContentProvider> void registerContentProvider(String authority, Class<T> providerClass) {
        ProviderInfo info = new ProviderInfo();
        info.authority = authority;
        Robolectric.buildContentProvider(providerClass).create(info);
    }
}
