
package app.crossword.yourealwaysbe.util.files;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import app.crossword.yourealwaysbe.puz.Puzzle;

public class MetaCache {

    public static class Converters {
        @TypeConverter
        public static LocalDate fromEpochDay(Long value) {
            return value == null ? null : LocalDate.ofEpochDay(value);
        }

        @TypeConverter
        public static Long dateToEpochDay(LocalDate date) {
            return date == null ? null : date.toEpochDay();
        }

        @TypeConverter
        public static Uri fromUriString(String value) {
            return value == null ? null : Uri.parse(value);
        }

        @TypeConverter
        public static String uriToString(Uri uri) {
            return uri == null ? null : uri.toString();
        }
    }

    @Entity(tableName = "cachedMeta")
    @TypeConverters({Converters.class})
    public static class CachedMeta {
        @PrimaryKey
        @NonNull
        public Uri mainFileUri;

        // currently unused but may be used to speed up dir listing in
        // browse activity
        @ColumnInfo
        public Uri metaFileUri;

        @ColumnInfo
        @NonNull
        public Uri directoryUri;

        @ColumnInfo
        public boolean isUpdatable;

        @ColumnInfo
        public LocalDate date;

        @ColumnInfo
        public int percentComplete;

        @ColumnInfo
        public int percentFilled;

        @ColumnInfo
        public String source;

        @ColumnInfo
        public String title;

        // from db version 2
        @ColumnInfo
        public String author;
    }

    @Dao
    @TypeConverters({Converters.class})
    public static abstract class CachedMetaDao {
        @Query("SELECT * FROM cachedMeta WHERE directoryUri = :directory")
        public abstract List<CachedMeta> getDirCache(Uri directory);

        @Query("SELECT * FROM cachedMeta WHERE mainFileUri = :mainFileUri")
        public abstract CachedMeta getCache(Uri mainFileUri);

        @Query("SELECT mainFileUri FROM cachedMeta WHERE directoryUri = :directory")
        public abstract List<Uri> getDirFileUris(Uri directory);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        public abstract void insertAll(CachedMeta... metas);

        @Query("DELETE FROM cachedMeta WHERE mainFileUri = :mainFileUri")
        public abstract void delete(Uri mainFileUri);

        @Delete
        public abstract void delete(CachedMeta cm);

        /**
         * Delete files in dir not in puzMetaFiles
         */
        @Transaction
        public void deleteOutside(
            Uri directory, Collection<PuzMetaFile> puzMetaFiles
        ) {
            Set<Uri> keepUris = new HashSet<>(puzMetaFiles.size());
            for (PuzMetaFile pm : puzMetaFiles) {
                keepUris.add(pm.getPuzHandle().getMainFileHandle().getUri());
            }

            for (CachedMeta cm : getDirCache(directory)) {
                if (!keepUris.contains(cm.mainFileUri)) {
                    delete(cm);
                }
            }
        }
    }

    @Database(entities = {CachedMeta.class}, version = 2)
    public static abstract class CachedMetaDB extends RoomDatabase {
        private static CachedMetaDB instance = null;

        /**
         * Version 2 adds an author column
         *
         * Thanks to
         * https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929
         */
        private static final Migration MIGRATION_l_2 = new Migration(1, 2) {
            @Override
            public void migrate(SupportSQLiteDatabase database) {
                database.execSQL(
                    "ALTER TABLE cachedMeta ADD COLUMN author TEXT"
                );
            }
        };

        public static CachedMetaDB getInstance(Context applicationContext) {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    applicationContext, CachedMetaDB.class, "meta-cache-db"
                ).addMigrations(MIGRATION_l_2)
                .build();
            }
            return instance;
        }

        public abstract CachedMetaDao cachedMetaDao();
    }

    public class MetaRecord {
        private CachedMeta dbRow;

        public MetaRecord(CachedMeta dbRow) {
            this.dbRow = dbRow;
        }

        public boolean isUpdatable() { return dbRow.isUpdatable; }
        public String getCaption() { return dbRow.title; }
        public LocalDate getDate() { return dbRow.date; }
        public int getPercentComplete() { return dbRow.percentComplete; }
        public int getPercentFilled() { return dbRow.percentFilled; }
        public String getSource() { return dbRow.source; }
        public String getTitle() { return dbRow.title; }
        public String getAuthor() { return dbRow.author; }
    }

    private Context applicationContext;
    private FileHandler fileHandler;

    public MetaCache(Context applicationContext, FileHandler fileHandler) {
        this.applicationContext = applicationContext;
        this.fileHandler = fileHandler;
    }

    /**
     * Returns all cached meta data records for the given directory
     */
    public Map<Uri, MetaRecord> getDirCache(DirHandle dirHandle) {
        Uri dirUri = fileHandler.getUri(dirHandle);
        Map<Uri, MetaRecord> cache = new HashMap<>();
        for (CachedMeta cm : getDao().getDirCache(dirUri)) {
            cache.put(cm.mainFileUri, new MetaRecord(cm));
        }
        return  cache;
    }

    /**
     * Return cached meta for given handle
     *
     * @return null if no entry in cache
     */
    public MetaRecord getCache(Uri puzFileUri) {
        CachedMeta cm = getDao().getCache(puzFileUri);
        return (cm == null) ? null : new MetaRecord(cm);
    }

    /**
     * Cache meta for a file URI, returns new record
     */
    public MetaRecord addRecord(PuzHandle puzHandle, Puzzle puz) {
        CachedMeta cm = new CachedMeta();
        cm.mainFileUri = fileHandler.getUri(puzHandle.getMainFileHandle());

        FileHandle metaHandle
            = puzHandle.accept(new PuzHandle.Visitor<FileHandle>() {
                public FileHandle visit(PuzHandle.Puz puzPH) {
                    return puzPH.getMetaFileHandle();
                }
                public FileHandle visit(PuzHandle.IPuz ipuzPH) {
                    return null;
                }
            });

        cm.metaFileUri = (metaHandle == null)
            ? null
            : fileHandler.getUri(metaHandle);

        cm.directoryUri = fileHandler.getUri(puzHandle.getDirHandle());
        cm.isUpdatable = puz.isUpdatable();
        cm.date = puz.getDate();
        cm.percentComplete = puz.getPercentComplete();
        cm.percentFilled = puz.getPercentFilled();
        cm.source = puz.getSource();
        cm.title = puz.getTitle();
        cm.author = puz.getAuthor();

        getDao().insertAll(cm);

        return new MetaRecord(cm);
    }

    /**
     * Remove a record from the cache
     */
    public void deleteRecord(PuzHandle puzHandle) {
        getDao().delete(fileHandler.getUri(puzHandle.getMainFileHandle()));
    }

    /**
     * Remove all records from directory that are not in the handles
     */
    public void cleanupCache(
        DirHandle dir, List<PuzMetaFile> puzMetaFiles
    ) {
        Uri dirUri = fileHandler.getUri(dir);
        getDao().deleteOutside(dirUri, puzMetaFiles);
    }

    private CachedMetaDao getDao() {
        return CachedMetaDB.getInstance(applicationContext).cachedMetaDao();
    }
}
