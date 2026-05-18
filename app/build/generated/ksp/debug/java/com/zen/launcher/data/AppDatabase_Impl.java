package com.zen.launcher.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile AllowedAppDao _allowedAppDao;

  private volatile NotificationRuleDao _notificationRuleDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `allowed_apps` (`packageName` TEXT NOT NULL, `label` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`packageName`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `notification_rules` (`packageName` TEXT NOT NULL, `label` TEXT NOT NULL, `enabled` INTEGER NOT NULL, PRIMARY KEY(`packageName`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '33e6f46c5d53ad6031d61968c7016601')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `allowed_apps`");
        db.execSQL("DROP TABLE IF EXISTS `notification_rules`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsAllowedApps = new HashMap<String, TableInfo.Column>(3);
        _columnsAllowedApps.put("packageName", new TableInfo.Column("packageName", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAllowedApps.put("label", new TableInfo.Column("label", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAllowedApps.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAllowedApps = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAllowedApps = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAllowedApps = new TableInfo("allowed_apps", _columnsAllowedApps, _foreignKeysAllowedApps, _indicesAllowedApps);
        final TableInfo _existingAllowedApps = TableInfo.read(db, "allowed_apps");
        if (!_infoAllowedApps.equals(_existingAllowedApps)) {
          return new RoomOpenHelper.ValidationResult(false, "allowed_apps(com.zen.launcher.data.AllowedApp).\n"
                  + " Expected:\n" + _infoAllowedApps + "\n"
                  + " Found:\n" + _existingAllowedApps);
        }
        final HashMap<String, TableInfo.Column> _columnsNotificationRules = new HashMap<String, TableInfo.Column>(3);
        _columnsNotificationRules.put("packageName", new TableInfo.Column("packageName", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationRules.put("label", new TableInfo.Column("label", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotificationRules.put("enabled", new TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNotificationRules = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesNotificationRules = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoNotificationRules = new TableInfo("notification_rules", _columnsNotificationRules, _foreignKeysNotificationRules, _indicesNotificationRules);
        final TableInfo _existingNotificationRules = TableInfo.read(db, "notification_rules");
        if (!_infoNotificationRules.equals(_existingNotificationRules)) {
          return new RoomOpenHelper.ValidationResult(false, "notification_rules(com.zen.launcher.data.NotificationRule).\n"
                  + " Expected:\n" + _infoNotificationRules + "\n"
                  + " Found:\n" + _existingNotificationRules);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "33e6f46c5d53ad6031d61968c7016601", "94715288852d42ddbc5f0fd7b31732b7");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "allowed_apps","notification_rules");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `allowed_apps`");
      _db.execSQL("DELETE FROM `notification_rules`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(AllowedAppDao.class, AllowedAppDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(NotificationRuleDao.class, NotificationRuleDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public AllowedAppDao allowedAppDao() {
    if (_allowedAppDao != null) {
      return _allowedAppDao;
    } else {
      synchronized(this) {
        if(_allowedAppDao == null) {
          _allowedAppDao = new AllowedAppDao_Impl(this);
        }
        return _allowedAppDao;
      }
    }
  }

  @Override
  public NotificationRuleDao notificationRuleDao() {
    if (_notificationRuleDao != null) {
      return _notificationRuleDao;
    } else {
      synchronized(this) {
        if(_notificationRuleDao == null) {
          _notificationRuleDao = new NotificationRuleDao_Impl(this);
        }
        return _notificationRuleDao;
      }
    }
  }
}
