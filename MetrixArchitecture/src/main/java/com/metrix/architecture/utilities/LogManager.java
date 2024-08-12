package com.metrix.architecture.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.metrix.architecture.assistants.MetrixApplicationAssistant;
import com.metrix.architecture.database.MobileApplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.util.Log;

/**
 * The LogManager class allows developers to write notes to a log
 * file named MobileTrace.log. This is commonly used to get diagnostic
 * information from a device in the field.
 *
 * @since 5.4
 */
public class LogManager {
	private static LogManager mInstance = null;
	private static final int MAX_FILE_SIZE = 4194304;
	private static final String LOG_FILE_NAME = "MobileTrace.log";
	private static final String zippedLogFile = "Mobile_log.zip";
	private static final String zippedDatabaseFile = "Mobile_database.zip";

	/**
	 * The logging level the message should be recorded as.
	 */
	public enum Level { DEBUG, INFO, WARN, ERROR }

	private Level mLevel = Level.ERROR;
	private int mMaxLogs = 10;
	private String mTag;
	private SimpleDateFormat mFormat = new SimpleDateFormat("MM/dd/yy hh:mm:ss a", Locale.US);
	private File mLog;
	private PrintWriter mWriter;
	public static String mSupportEmail = ""; // in 5.6, get from a metrix app param

	private static boolean mLoggingOn = true;

	private void LoggerSetup(String tag, String logFilename, Level level ) {
		try {
			//close previous
			if( mWriter != null ) {
				mWriter.flush();
				mWriter.close();
				mWriter = null;
			}
		}catch(Exception ex){
			Log.e("Logging", ex.getMessage());
		}
		//open new
		this.mTag = tag;
		this.mLog = createWriter( logFilename );
		this.mLevel = level;
		this.mMaxLogs = SettingsHelper.getMaxLogs(MetrixPublicCache.instance.getApplicationContext());
	}

	public LogManager(Context context) {
		LoggerSetup(context.getPackageName(), "MobileTrace.log", Level.ERROR);
	}

	public static LogManager getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new LogManager(context);
		}
		return mInstance;
	}

	public static LogManager getInstance() {
		if (mInstance == null) {
			mInstance = new LogManager();
		}
		return mInstance;
	}

	public LogManager() {
		LoggerSetup(MobileApplication.getAppContext().getPackageName(), "MobileTrace.log", Level.ERROR);
	}

	/**
	 * @param logFilename
	 * @return
	 */
	private File createWriter(String logFilename) {
		try {
			String state = Environment.getExternalStorageState();
			if( state.equals(Environment.MEDIA_MOUNTED) ) {
				File dir = new File( MobileApplication.getAppContext().getExternalFilesDir(null), "");
				if( !dir.exists() ) {
					Log.w(mTag, "Could not get log directory: " + dir.getAbsolutePath() );
					dir.mkdir();
				}
				File log = new File(dir, logFilename);
				if( log.exists() ) {
					if(log.length()>= MAX_FILE_SIZE)
						rotate( log );
				}
				Log.i(mTag, " Opening " + log.getAbsolutePath() );
				mWriter = new PrintWriter( new FileWriter( log, true ), true );
				return log;
			} else {
				Log.w(mTag, "Could not create log file because external storage state was " + state);
			}
		} catch( IOException ioe ) {
			Log.e(mTag, "Failed while opening the log file.", ioe );
		}

		return null;
	}

	/**
	 * rotate log files
	 * @param log
	 */
	private void rotate(File log) {
		int index = log.getName().lastIndexOf('.');
		if( index < 0 ) index = log.getName().length();
		String prefix = log.getName().substring(0, index );
		String extension = log.getName().substring(index);

		int lastLog = mMaxLogs - 1;
		File lastLogFile = new File( log.getParentFile(), prefix + "-" + lastLog + extension );
		if( lastLogFile.exists() ) lastLogFile.delete();

		for( int i = lastLog; i >= 1; --i ) {
			String filename = prefix + "-" + i + extension;
			File l = new File( log.getParentFile(), filename );
			if( l.exists() ) {
				File newLog = new File( log.getParentFile(), prefix + "-" + (i+1) + extension );
				l.renameTo( newLog );
			}
		}

		log.renameTo( new File( log.getParentFile(), prefix + "-1" + extension ) );
	}

	/**
	 * Delete the log files
	 */
	public void delete() {
		Context mCtxt = MetrixPublicCache.instance.getApplicationContext();
		File dir = new File( mCtxt.getExternalFilesDir(null), "");
		if( !dir.mkdirs() ) {
			Log.w(mTag, "Could not create log directory: " + dir.getAbsolutePath() );
		}
		File log = new File(dir, LOG_FILE_NAME);

		int index = log.getName().lastIndexOf('.');
		if( index < 0 ) index = log.getName().length();
		String prefix = log.getName().substring(0, index );
		String extension = log.getName().substring(index);

		int lastLog = mMaxLogs - 1;
		File lastLogFile = new File( log.getParentFile(), prefix + "-" + lastLog + extension );
		if( lastLogFile.exists() ) lastLogFile.delete();
		if(log.exists()) log.delete();

		for( int i = lastLog; i >= 1; --i ) {
			String filename = prefix + "-" + i + extension;
			File l = new File( log.getParentFile(), filename );
			if( l.exists() ) {
				l.delete();
			}
		}

		LoggerSetup(MobileApplication.getAppContext().getPackageName(), "MobileTrace.log", Level.ERROR);
	}

	public Level getLevel() {
		return mLevel;
	}

	public void setMaxLogs() {
		File path = Environment.getDataDirectory();
		StatFs dataStats = new StatFs(path.getPath());
		long blockSize = dataStats.getBlockSize();
		long availableBlocks = dataStats.getAvailableBlocks();
		long availableMB = (blockSize * availableBlocks) / 1024 / 1024;

		// logs are 4 MB in size ... use 1/3 of available space (max out at 400 MB)
		int maxLogs = (int) availableMB / 4 / 3;
		if (maxLogs > 100) {
			maxLogs = 100;
		}

		this.mMaxLogs = maxLogs;
		SettingsHelper.saveMaxLogs(MetrixPublicCache.instance.getApplicationContext(), maxLogs);
	}

	public void setLevel(Level level) {
		this.mLevel = level;
	}

	public boolean isLoggable( Level level ) {
		return level.ordinal() >= this.mLevel.ordinal();
	}

	public static void setLoggingOn(boolean mLoggingOn) {
		LogManager.mLoggingOn = mLoggingOn;
	}

	public static boolean isLoggingOn() {
		return mLoggingOn;
	}

	/**
	 * @param message
	 * @param parameters
	 */
	public void debug( String message, Object... parameters ) {
		if(MetrixStringHelper.isNullOrEmpty(message))
			return;

		if( parameters != null && parameters.length > 0 ) {
			Log.d( mTag, MessageFormat.format( message, parameters ) );
			log( Level.DEBUG, message, parameters );
		} else {
			Log.d( mTag, message );
			log( Level.DEBUG, message);
		}
	}

	/**
	 * @param message
	 * @param parameters
	 */
	public void info( String message, Object... parameters ) {
		if(MetrixStringHelper.isNullOrEmpty(message))
			return;

		if( parameters != null && parameters.length > 0 ) {
			Log.i(mTag, MessageFormat.format(message, parameters));
			log( Level.INFO, message, parameters );
		} else {
			Log.i(mTag, message);
			log( Level.INFO, message );
		}
	}

	/**
	 * @param message
	 * @param parameters
	 */
	public void warn( String message, Object... parameters ) {
		if(MetrixStringHelper.isNullOrEmpty(message))
			return;

		if( parameters != null && parameters.length > 0 ) {
			Log.w(mTag, MessageFormat.format(message, parameters));
			log( Level.WARN, message, parameters );
		} else {
			Log.w(mTag, message);
			log( Level.WARN, message);
		}
	}

	/**
	 * @param message
	 * @param parameters
	 */
	public void error( String message, Object... parameters ) {
		if(MetrixStringHelper.isNullOrEmpty(message))
			return;

		if( parameters != null && parameters.length > 0 ) {
			Log.e(mTag, MessageFormat.format(message, parameters));
			log( Level.ERROR, message, parameters );
		} else {
			Log.e(mTag, message);
			log( Level.ERROR, message);
		}
	}

	/**
	 * @param throwable
	 */
	public void error(Throwable throwable) {
		String message = Log.getStackTraceString( throwable );

		if(MetrixStringHelper.isNullOrEmpty(message))
			return;

		Log.e( mTag, message, throwable );
		log( Level.ERROR, message );
	}

	/**
	 * Close the file writer
	 */
	public void close() {
		try {
			if( mWriter != null ) {
				mWriter.flush();
				mWriter.close();
				mWriter = null;
			}
		}catch(Exception ex){
			Log.e( mTag, ex.getMessage());
		}
	}

	/**
	 * @param level
	 * @param message
	 * @param parameters
	 */
	private void log( Level level, String message, Object... parameters ) {
		Context context = MetrixPublicCache.instance.getApplicationContext();

		// Since with Android 11 (API 30) and up, we are trying to save all files into a private
		// directory, which we don't need permission to do.
		if (context == null || (Build.VERSION.SDK_INT < 30 && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
			return;

		if (this.mLog == null)
			LoggerSetup(context.getPackageName(), "MobileTrace.log", Level.ERROR);

		if (!mLog.exists()) {
			this.mLog = createWriter(LogManager.LOG_FILE_NAME );
		}

		if(this.mLog != null && this.mLog.length() >= MAX_FILE_SIZE) {
			rotate( this.mLog );
			this.mLog = createWriter(LogManager.LOG_FILE_NAME );
		}

		if( mWriter != null && isLoggingOn() && isLoggable(level) ) {
			Date date = new Date();
			try {
				mWriter.print(mFormat.format(date) );
				mWriter.print( "|" );
				mWriter.print( level.toString() );
				mWriter.print( "|" );
				mWriter.print( getMethodInfo(4));
				mWriter.print( "|" );
				mWriter.print( Thread.currentThread().getName() );
				mWriter.print( "|" );
				if( parameters != null && parameters.length > 0 ) {
					mWriter.println(MessageFormat.format( message+"\r\n", parameters ) );
				} else {
					mWriter.println(message+"\r\n");
				}
			}catch(Exception ex){
				Log.e( mTag, ex.getMessage());
			}
		}
		else if( mWriter == null && isLoggingOn() && isLoggable(level)){
			this.mLog = createWriter(LogManager.LOG_FILE_NAME );

			Date date = new Date();
			try {
				mWriter.print(mFormat.format(date) );
				mWriter.print( "|" );
				mWriter.print( level.toString() );
				mWriter.print( "|" );
				mWriter.print( getMethodInfo(4));
				mWriter.print( "|" );
				mWriter.print( Thread.currentThread().getName() );
				mWriter.print( "|" );
				if( parameters != null && parameters.length > 0 ) {
					mWriter.println(MessageFormat.format( message+"\r\n", parameters ) );
				} else {
					mWriter.println(message+"\r\n");
				}
			}catch(Exception ex){
				Log.e( mTag, ex.getMessage());
			}
		}
	}

	/**
	 * @param context
	 * @throws IOException
	 */
	public void sendEmail(Context context) throws IOException {
		if (MetrixStringHelper.isNullOrEmpty(mSupportEmail)) {
			mSupportEmail = MobileApplication.getAppParam("SUPPORT_EMAIL");
		}
		sendEmail(context, mSupportEmail, false);
	}

	/**
	 * @param context
	 * @param fatalError
	 * @throws IOException
	 */
	public void sendEmail(Context context, boolean fatalError) throws IOException {
		if (MetrixStringHelper.isNullOrEmpty(mSupportEmail)) {
			mSupportEmail = MobileApplication.getAppParam("SUPPORT_EMAIL");
		}
		sendEmail(context, mSupportEmail, fatalError);
	}

	/**
	 * @param context
	 * @param emailAddress
	 * @param fatalError
	 * @throws IOException
	 */
	public void sendEmail(Context context, String emailAddress, boolean fatalError) throws IOException {
		if(mLog == null) {
			MetrixUIHelper.showErrorDialogOnGuiThread((Activity)context, AndroidResourceHelper.getMessage("LoggerNotAvailForOp"));
			return;
		}

		close();

		File[] logs = mLog.getParentFile().listFiles(
				new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pathname.getName().endsWith(".log");
					}
				});

		File temp = zipLogFiles(logs);
		String[] mailto = { emailAddress };
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.setType("application/zip");
		sendIntent.putExtra(Intent.EXTRA_EMAIL, mailto);
		Uri attachmentURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", temp);
		sendIntent.putExtra(Intent.EXTRA_STREAM, attachmentURI );
		sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		sendIntent.setType("text/plain");
		if (fatalError) {
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, AndroidResourceHelper.getMessage("FatalLogFileAttached1Args", mTag));
			sendIntent.putExtra(Intent.EXTRA_TEXT, AndroidResourceHelper.getMessage("AUserExpAFatal"));
			Intent newTaskIntent = Intent.createChooser(sendIntent, AndroidResourceHelper.getMessage("FatalErrSendLogsTo"));
			newTaskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(newTaskIntent);
		} else {
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, AndroidResourceHelper.getMessage("LogFileAttached1Args", mTag ));
			sendIntent.putExtra(Intent.EXTRA_TEXT, AndroidResourceHelper.getMessage("AUserHasReqYou"));
			context.startActivity(Intent.createChooser(sendIntent, AndroidResourceHelper.getMessage("SendLogsToSupport")));
		}
	}

	/**
	 * @param context
	 * @throws IOException
	 */
	public void sendDatabase(Context context) throws IOException {
		if (MetrixStringHelper.isNullOrEmpty(mSupportEmail)) {
			mSupportEmail = MobileApplication.getAppParam("SUPPORT_EMAIL");
		}
		sendDatabase(context, mSupportEmail, false);
	}

	/**
	 * @param context
	 * @param emailAddress
	 * @param fatalError
	 * @throws IOException
	 */
	public void sendDatabase(Context context, String emailAddress, boolean fatalError) throws IOException {
		if(mLog == null) {
			MetrixUIHelper.showErrorDialogOnGuiThread((Activity)context, AndroidResourceHelper.getMessage("LoggerNotAvailForOp"));
			return;
		}

		close();

		File[] dbFiles = mLog.getParentFile().listFiles(
				new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pathname.getName().endsWith(".db");
					}
				});

		File temp = zipDatabaseFiles(dbFiles);
		String[] mailto = { emailAddress };
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.setType("application/zip");
		sendIntent.putExtra(Intent.EXTRA_EMAIL, mailto);
		Uri attachmentURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", temp);
		sendIntent.putExtra(Intent.EXTRA_STREAM, attachmentURI );
		sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		sendIntent.putExtra(Intent.EXTRA_STREAM, attachmentURI);
		sendIntent.setType("text/plain");
		if (fatalError) {
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, mTag + " (Fatal): Database File Attached");
			sendIntent.putExtra(Intent.EXTRA_TEXT, "A user has experienced data error and has attached a database containing relevant information.");
			Intent newTaskIntent = Intent.createChooser(sendIntent, "Fatal Error: Send database to Support");
			newTaskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(newTaskIntent);
		} else {
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, mTag + ": Database File Attached");
			sendIntent.putExtra(Intent.EXTRA_TEXT, "A user has requested you look at database.");
			context.startActivity(Intent.createChooser(sendIntent, "Send database To Support"));
		}
	}

	/**
	 * @param logs
	 * @return
	 * @throws IOException
	 */
	private File zipLogFiles(File[] logs) throws IOException {
		return this.zipFiles(logs, zippedLogFile);
	}

	/**
	 * @param databaseFile
	 * @return
	 * @throws IOException
	 */
	private File zipDatabaseFiles(File[] databaseFile) throws IOException {
		return this.zipFiles(databaseFile, zippedDatabaseFile);
	}

	/**
	 * @param files
	 * @return
	 * @throws IOException
	 */
	private File zipFiles(File[] files, String zipFileName) throws IOException {
		Context mCtxt = MetrixPublicCache.instance.getApplicationContext();
		File dir = new File( mCtxt.getExternalFilesDir(null), "");
		File zipfile = new File(dir, zipFileName);

		ZipOutputStream stream = new ZipOutputStream( new FileOutputStream(zipfile) );
		try {
			for( File f : files ) {
				ZipEntry entry = new ZipEntry( f.getName() );
				stream.putNextEntry( entry );
				copy( stream, f );
				stream.closeEntry();
			}
			stream.finish();
			return zipfile;
		} finally {
			stream.close();
		}
	}

	/**
	 * @param stream
	 * @param file
	 * @throws IOException
	 */
	private void copy(OutputStream stream, File file) throws IOException {
		InputStream istream = new FileInputStream( file );
		try {
			byte[] buffer = new byte[8096];
			int length = 0;
			while( (length = istream.read( buffer )) >= 0 ) {
				stream.write( buffer, 0, length );
			}
		} finally {
			istream.close();
		}
	}

	/**
	 * @param depth
	 * @return
	 */
	public static String getMethodInfo(final int depth)	{
		String methodInfo = "";
		try {
			final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
			return methodInfo=ste[1 + depth].getClassName()+":"+ste[1 + depth].getMethodName();
		}catch(Exception ex){
			return methodInfo;
		}
	}
}