package uk.co.bithatch.snake.updater;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import com.sshtools.forker.updater.UninstallHandler;
import com.sshtools.forker.updater.UninstallSession;
import com.sshtools.forker.updater.test.UninstallTest;

public class JavaFXUninstallHandler implements UninstallHandler {

	public static void main(String[] args) throws Exception {
		UninstallTest.main(args, new JavaFXUninstallHandler());
	}

	private static JavaFXUninstallHandler instance;

	public static JavaFXUninstallHandler get() {
		if (instance == null)
			/* For when launching from development environment */
			instance = new JavaFXUninstallHandler();
		return instance;
	}

	private UninstallHandler delegate;
	private Semaphore flag = new Semaphore(1);
	private UninstallSession session;

	public JavaFXUninstallHandler() {
		instance = this;
		flag.acquireUninterruptibly();
	}

	@Override
	public boolean isCancelled() {
		return delegate.isCancelled();
	}

	@Override
	public Boolean prep(Callable<Void> callable) {
		flag.acquireUninterruptibly();
		try {
			return delegate.prep(callable);
		} finally {
			flag.release();
		}
	}

	@Override
	public Boolean value() {
		flag.acquireUninterruptibly();
		try {
			return delegate.value();
		} finally {
			flag.release();
		}
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
	public void failed(Throwable error) {
		flag.acquireUninterruptibly();
		try {
			delegate.failed(error);
		} finally {
			flag.release();
		}
	}

	public UninstallSession getSession() {
		return session;
	}

	@Override
	public void init(UninstallSession session) {
		this.session = session;
		new Thread() {
			public void run() {
				try {
					Bootstrap.main(new String[0]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	@Override
	public void uninstallFile(Path file, Path d, int index) throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.uninstallFile(file, d, index);
		} finally {
			flag.release();
		}
	}

	@Override
	public void uninstallDone() {
		flag.acquireUninterruptibly();
		try {
			delegate.uninstallDone();
		} finally {
			flag.release();
		}
	}

	@Override
	public void uninstallFileDone(Path file) throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.uninstallFileDone(file);
		} finally {
			flag.release();
		}

	}

	@Override
	public void uninstallFileProgress(Path file, float progress) throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.uninstallFileProgress(file, progress);
		} finally {
			flag.release();
		}

	}

	@Override
	public void uninstallProgress(float progress) throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.uninstallProgress(progress);
		} finally {
			flag.release();
		}
	}

	public boolean isActive() {
		return session != null;
	}

	public void setDelegate(UninstallHandler delegate) {
		if (this.delegate != null)
			throw new IllegalStateException("Delegate already set.");
		this.delegate = delegate;
		delegate.init(session);
		flag.release();
	}

	@Override
	public void startUninstall() throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.startUninstall();
		} finally {
			flag.release();
		}
	}

}
