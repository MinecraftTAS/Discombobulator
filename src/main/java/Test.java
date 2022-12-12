import java.nio.file.Path;
import java.nio.file.Paths;

public class Test {

	public static void main(String[] args) throws Exception {
		FileWatcher test = new FileWatcher(Paths.get("C:\\Users\\games\\Downloads\\test")) {
			@Override
			protected void onNewFile(Path path) {
				System.out.println("New file: " + path.toFile().getName());
			}

			@Override
			protected void onModifyFile(Path path) {
				System.out.println("Modify file: " + path.toFile().getName());
			}

			@Override
			protected void onDeleteFile(Path path) {
				System.out.println("Delete file: " + path.toFile().getName());
			}
		};
		test.watch();
	}
}
