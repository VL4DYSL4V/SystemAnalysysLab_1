package dao;

import framework.exception.LaboratoryFrameworkException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.linear.RealVector;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class FileSystemVectorXDao implements VectorXDao {

    private static final Path directoryPath;

    private final Path path;

    static {
        String userHome = System.getProperty("user.home");
        String directoryPathString = userHome + File.separator +
                "Documents" + File.separator +
                "SystemAnalysisLab_1";
        directoryPath = Paths.get(directoryPathString);
        try {
            FileUtils.forceMkdir(directoryPath.toFile());
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public FileSystemVectorXDao(String fileName) {
        this.path = directoryPath.resolve(fileName);
        try {
            FileUtils.touch(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        clearFile(path);
    }

    @Override
    public void write(int iterationStep, RealVector x) {
        try {
            String s = (Files.size(path) == 0) ? getHeader(x.getDimension()) : getRow(iterationStep, x);
            Files.write(path, s.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getRow(int iterationStep, RealVector x){
        StringBuilder sb = new StringBuilder(100);
        sb.append(iterationStep).append("\t");
        for (int i = 0; i < x.getDimension(); i++){
            sb.append(x.getEntry(i)).append("\t");
        }
        sb.delete(sb.length() - 1, sb.length());
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    private String getHeader(int length) {
        StringBuilder sb = new StringBuilder("k\t\t");
        for (int i = 0; i < length; i++){
            sb.append("x_").append(i + 1).append("\t\t\t");
        }
        sb.delete(sb.length() - 3, sb.length());
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    private void clearFile(Path path) {
        try {
            Files.write(path, "".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new LaboratoryFrameworkException(e);
        }
    }

}
