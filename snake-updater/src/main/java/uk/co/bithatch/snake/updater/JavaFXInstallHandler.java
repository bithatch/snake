package uk.co.bithatch.snake.updater;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import com.sshtools.forker.updater.InstallHandler;
import com.sshtools.forker.updater.InstallSession;

public class JavaFXInstallHandler implements InstallHandler {

	private static JavaFXInstallHandler instance;

	public static JavaFXInstallHandler get() {
		if (instance == null)
			/* For when launching from development environment */
			instance = new JavaFXInstallHandler();
		return instance;
	}
	
	private InstallHandler delegate;
	private Semaphore flag = new Semaphore(1);
	private InstallSession session;

	public JavaFXInstallHandler() {
		instance = this;
		flag.acquireUninterruptibly();
	}
	
	@Override
	public Path chooseDestination(Callable<Void> callable) {
		flag.acquireUninterruptibly();
		try {
			return delegate.chooseDestination(callable);
		} finally {
			flag.release();
		}
	}

	@Override
	public Path chosenDestination() {
		flag.acquireUninterruptibly();
		try {
			return delegate.chosenDestination();
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

	public InstallSession getSession() {
		return session;
	}

	@Override
	public void init(InstallSession session) {
		this.session = session;
		new Thread() {
			public void run() {
				try {
					Bootstrap.main(session.updater().getArguments());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	@Override
	public void installFile(Path file, Path d, int index) throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.installFile(file, d, index);
		} finally {
			flag.release();
		}
	}

	@Override
	public void installDone() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void installFileDone(Path file) throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.installFileDone(file);
		} finally {
			flag.release();
		}

	}

	@Override
	public void installFileProgress(Path file, float progress) throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.installFileProgress(file, progress);
		} finally {
			flag.release();
		}

	}

	@Override
	public void installProgress(float progress) throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.installProgress(progress);
		} finally {
			flag.release();
		}
	}

	public boolean isActive() {
		return session != null;
	}

	public void setDelegate(InstallHandler delegate) {
		if (this.delegate != null)
			throw new IllegalStateException("Delegate already set.");
		this.delegate = delegate;
		delegate.init(session);
		flag.release();
	}

	@Override
	public void startInstall() throws Exception {
		flag.acquireUninterruptibly();
		try {
			delegate.startInstall();
		} finally {
			flag.release();
		}
	}

}
