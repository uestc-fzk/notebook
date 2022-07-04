package util;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 这里主要是记录的目录的相关工具方法，对于单个文件的操作FileInputStream可能已经够了
 * 主要用到 Files 工具类和 Path 类
 *
 * @author fzk
 * @date 2021-11-23 23:40
 */
@SuppressWarnings("unused")
public class MyFileUtil {

    public static void writeToFile(byte[] bytes, String dirPath, String fileName) throws IOException {
        Path filePath = Path.of(dirPath, fileName);
        writeToPath(filePath, bytes);
    }

    public static void writeToPath(Path filePath, byte[] bytes) throws IOException {
        writeToPath(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    public static void writeToPath(Path filePath, byte[] bytes, StandardOpenOption... options) throws IOException {
        assert filePath != null : "path can not be null";

        // 确保目录及文件已经创建
        if (Files.notExists(filePath.getParent()))
            Files.createDirectories(filePath.getParent());// 创建所有不存在的父目录来创建目录
        // 文件本来也是需要判断创建的，不过默认以StandardOpenOption.CREATE方式打开文件的话就不需要创建了
        if (Files.notExists(filePath))
            Files.createFile(filePath);

        Files.write(filePath, bytes, options);
    }

    public static void writeString(Path filePath, String toWrite) throws IOException {
        writeString(filePath, toWrite, StandardCharsets.UTF_8);
    }

    public static void writeString(Path filePath, String toWrite, Charset charset) throws IOException {
        assert filePath != null : "path can not be null";
        assert toWrite != null : "string to write to file can not be null";

        byte[] bytes = toWrite.getBytes(charset);
        writeToPath(filePath, bytes);
    }

    /*在targetSource中某个子文件或目录已经存在的情况下会抛出异常，即不支持覆盖*/
    public static void copyDir(Path sourceDir, Path targetDir) throws IOException {
        copyDir(sourceDir, targetDir, false);
    }

    /**
     * 复制文件目录
     *
     * @param sourceDir 源目录
     * @param targetDir 目标目录
     * @param isCovered 是否覆盖
     * @apiNote 不知道为什么，如果设为true，windows的文件不但手动难以删除，这里也会被一直卡主
     */
    public static void copyDir(Path sourceDir, Path targetDir, boolean isCovered) throws IOException {
        assert sourceDir != null && targetDir != null : "sourceDir or targetDir is null";

        // 1.判断是否是目录：不存在或存在而不是目录都返回false
        if (!Files.isDirectory(sourceDir))
            throw new IllegalArgumentException("sourceDir is not a directory or it not exists");

        if (Files.exists(targetDir) && !Files.isDirectory(targetDir))
            throw new IllegalArgumentException("targetDir exists but not a directory");
        // 这里可以创建到targetSource的父级目录
        if (Files.notExists(targetDir.getParent()))
            Files.createDirectories(targetDir.getParent());

        // 2.前面没有问题就正式开始copy了
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            stream.forEach(p -> {
                try {
                    // 找到相对于target的绝对文件path，resolve方法在解析path时，如果是绝对路径则直接返回，否则转为相对于的绝对路径
                    Path q = targetDir.resolve(sourceDir.relativize(p));
                    if (Files.isDirectory(p)) {
                        try {
                            Files.createDirectory(q);// 这个方法会在目录已经存在的情况下会报错
                        } catch (FileAlreadyExistsException e) {
                            if (!isCovered) throw e;// 指定覆盖则忽略
                        }
                    } else {
                        if (isCovered) // 指定覆盖行为
                            Files.copy(p, q, StandardCopyOption.REPLACE_EXISTING);
                        else Files.copy(p, q);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    /**
     * 删除一个目录，必须在先删除其子文件和子目录，否则DirectoryNotEmptyException
     *
     * @param dirPath 目录
     * @throws IOException 可能的IO异常
     */
    public static void deleteDir(Path dirPath) throws IOException {
        // 1.先检查path是否合法
        if (Files.notExists(dirPath))
            throw new NoSuchFileException("dirPath not exists");
        if (!Files.isDirectory(dirPath))
            throw new NotDirectoryException("dirPath not a directory");

        // 这个方法遍历文件树，在目录不存在的时候不会抛出异常
        Files.walkFileTree(dirPath, new SimpleFileVisitor<>() {
            /**
             * 在一个目录被处理前调用
             * @param dir 目录引用
             * @param attrs 目录基本属性
             * @return 访问结果
             * @throws IOException 如果发生I/O错误
             */
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Objects.requireNonNull(dir);
                Objects.requireNonNull(attrs);

//                System.out.println("pre------: "+dir.toString());
                return FileVisitResult.CONTINUE;
            }

            /**
             * 遇到目录里的 文件 时的调用
             * @param file 对文件的引用
             * @param attrs 文件的基本属性
             * @return 访问结果
             * @throws IOException 如果发生 I/O 错误
             */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Objects.requireNonNull(file);
                Objects.requireNonNull(attrs);

                // 删除文件
//                System.out.println("file------: "+file.toString());
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            /**
             * 试图访问文件或目录发生错误时，如 没有权限
             * @param file 对文件的引用
             * @param exc 阻止访问文件的 I/O 异常
             * @return 访问结果
             * @throws IOException 如果发生 I/O 错误
             */
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                Objects.requireNonNull(file);
//                throw exc;
                return FileVisitResult.SKIP_SIBLINGS;// 遇到异常，采取跳过策略
            }

            /**
             * 在目录中的条目及其所有后代都被访问后调用 <br/>
             * 目录的迭代过早完成时（通过返回SKIP_SIBLINGS的visitFile方法，或迭代目录时的 I/O 错误），也会调用此方法
             * @param dir 目录引用
             * @param exc 如果目录的迭代完成且没有错误，则为null ； 否则导致目录迭代过早完成的I/O异常
             * @return 访问结果
             * @throws IOException 如果发生IO异常
             */
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Objects.requireNonNull(dir);
                if (exc != null)
                    throw exc;

                // 把目录给删了
//                System.out.println("post------: "+dir.toString());
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
