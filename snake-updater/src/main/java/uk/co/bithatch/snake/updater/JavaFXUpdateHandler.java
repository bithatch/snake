package uk.co.bithatch.snake.updater;

import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import com.sshtools.forker.updater.Entry;
import com.sshtools.forker.updater.UpdateHandler;
import com.sshtools.forker.updater.UpdateSession;

public class JavaFXUpdateHandler implements UpdateHandler {

	private static JavaFXUpdateHandler instance;

	public static JavaFXUpdateHandler get() {
		if (instance == null)
			/* For when launching from development environment */
			instance = new JavaFXUpdateHandler();
		return instance;
	}
	
	private UpdateHandler delegate;
	private Semaphore flag = new Semaphore(1);
	private UpdateSession updater;

	public JavaFXUpdateHandler() {
		instance = this;
		flag.acquireUninterruptibly();
	}

	@Override
	public void complete() {
		flag.acquireUninterruptibly();
		try {
			delegate.complete();
		} finally {
			flag.release();
		}
	}

	@Override
	public void completedManifestLoad(URL location) {
		flag.acquireUninterruptibly();
		try {
			delegate.completedManifestLoad(location);
		} finally {
			flag.release();
		}
	}

	@Override
	public void doneDownloadFile(Entry file) throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.doneDownloadFile(file);
		} finally {
			flag.release();
		}
	}

	@Override
	public void failed(Throwable error) {
		flag.acquireUninterruptibly();
		try {
			delegate.failed(error);
		} finally {
			flag.release();
		}
	}

	public UpdateSession getSession() {
		return updater;
	}

	@Override
	public void init(UpdateSession updater) {
		this.updater = updater;
		new Thread() {
			public void run() {
				try {
					Bootstrap.main(updater.updater().getArguments());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public boolean isActive() {
		return updater != null;
	}

	@Override
	public boolean noUpdates(Callable<Void> task) {
		flag.acquireUninterruptibly();
		try {
			return delegate.noUpdates(task);
		} finally {
			flag.release();
		}
	}

	public void setDelegate(UpdateHandler delegate) {
		if (this.delegate != null)
			throw new IllegalStateException("Delegate already set.");
		this.delegate = delegate;
		delegate.init(updater);
		flag.release();
	}

	@Override
	public void startDownloadFile(Entry file, int index) throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.startDownloadFile(file, index);
		} finally {
			flag.release();
		}
	}

	@Override
	public void updateDone(boolean upgradeError) {
	}

	@Override
	public void startDownloads() throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.startDownloads();
		} finally {
			flag.release();
		}
	}

	@Override
	public void startingManifestLoad(URL location) {
		flag.acquireUninterruptibly();
		try {
			delegate.startingManifestLoad(location);
		} finally {
			flag.release();
		}
	}

	@Override
	public void updateDownloadFileProgress(Entry entry, float progress) throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.updateDownloadFileProgress(entry, progress);
		} finally {
			flag.release();
		}

	}

	@Override
	public void updateDownloadProgress(float progress) throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.updateDownloadProgress(progress);
		} finally {
			flag.release();
		}

	}

	@Override
	public boolean updatesComplete(Callable<Void> task) throws Exception {
		flag.acquireUninterruptibly();
		try {
			return delegate.updatesComplete(task);
		} finally {
			flag.release();
		}
	}

}
