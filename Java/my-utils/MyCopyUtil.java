package util;

import java.io.*;

/**
 * @author fzk
 * @date 2021-11-23 0:10
 */
public class MyCopyUtil {
    /**
     * 深拷贝(deep copy) ： 采用序列化方式的深度拷贝
     *
     * @param origin 拷贝对象
     * @return 深拷贝对象
     * @throws CloneNotSupportedException 异常
     */
    public static Object deepCopy(Object origin) throws CloneNotSupportedException {
        assert origin != null : "不能拷贝null";
        if (!(origin instanceof Serializable))
            throw new CloneNotSupportedException("对象必须可序列化");

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();

            try (ObjectOutputStream out = new ObjectOutputStream(bout)) {
                out.writeObject(origin);
            }

            try (ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray())) {
                ObjectInputStream in = new ObjectInputStream(bin);
                return in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            CloneNotSupportedException e1 = new CloneNotSupportedException();
            e1.initCause(e);
            throw e1;
        }
    }
}
