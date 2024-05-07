package mapper.implementation;

import ru.hse.homework4.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MapperSerializer implements Mapper {

    boolean retainIdentity;

    public MapperSerializer(boolean retainIdentity) {
        this.retainIdentity = retainIdentity;
    }

    private Field getDeclaredFieldForReading(Class<?> clazz, String fieldName) throws Exception {
        Field foundField;
        // Сначала проверяется наличие переименования с помощью PropertyName.
        var renamedField = Arrays.stream(clazz.getDeclaredFields())
                .filter(f ->
                {
                    if (f.getAnnotation(PropertyName.class) != null) {
                        return f.getAnnotation(PropertyName.class).value().equals(fieldName);
                    } else {
                        return false;
                    }
                }).findFirst();
        if (renamedField.isPresent()) {
            foundField = renamedField.get();
        } else {
            // Иначе ищется поле с таким же именем.
            try {
                foundField = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                if (clazz.getAnnotation(Exported.class).unknownPropertiesPolicy() == UnknownPropertiesPolicy.IGNORE) {
                    return null;
                } else {
                    throw new Exception();
                }
            }
        }
        // В самом конце проверяется, есть ли у поля аннотация Ignored и является ли он Synthetic.
        if (foundField.getAnnotation(Ignored.class) != null || foundField.isSynthetic()
                || java.lang.reflect.Modifier.isStatic(foundField.getModifiers())) {
            return null;
        }
        return foundField;
    }

    private String getDeclaredFieldForWriting(Field field) {
        // Для записи нужно проверить PropertyName и Ignored.
        if (field.getAnnotation(PropertyName.class) != null) {
            return field.getAnnotation(PropertyName.class).value();
        }
        if (field.getAnnotation(Ignored.class) != null || field.isSynthetic()
                || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
            return null;
        }
        return field.getName();
    }

    private boolean inputHasFeatureOfRetainingIdentity(String input) {
        // Если retainIdentity было == true при записи, то первая строка input обязательно == "%0".
        return input.startsWith("%0");
    }

    private Map<Integer, String> readStringMapperFromLines(String input) {
        Map<Integer, String> mapper = new HashMap<>();
        // Заполняется mapper.
        Scanner scanner = new Scanner(input);
        // Первая строка пропускается.
        scanner.nextLine();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            int indexOfColon = line.indexOf(':');
            mapper.put(Integer.parseInt(line.substring(0, indexOfColon)), line.substring(indexOfColon + 2));
        }
        scanner.close();
        return mapper;
    }

    private String transformInputWithIdentityToWithout(String input) {
        Map<Integer, String> mapper = readStringMapperFromLines(input);
        // Последовательная замена всех %чисел на соответствующие строки в input.
        for (int i = 0; i < mapper.size(); ++i) {
            input = input.replaceAll("%" + i, mapper.get(i));
        }
        return input;
    }

    /**
     * Читает сохранённый экземпляр класса {@code clazz} из строки {@code input}
     * и возвращает восстановленный экземпляр класса {@code clazz}.
     * <p>
     * Пример вызова:
     *
     * <pre>
     * String input = """
     * {"comment":"Хорошая работа","resolved":false}""";
     * ReviewComment reviewComment =
     * mapper.readFromString(ReviewComment.class, input);
     * System.out.println(reviewComment);
     * </pre>
     *
     * @param clazz класс, сохранённый экземпляр которого находится в {@code input}
     * @param input строковое представление сохранённого экземпляра класса {@code
     *              clazz}
     * @return восстановленный экземпляр {@code clazz}
     */
    @Override
    public <T> T readFromString(Class<T> clazz, String input) {
        // Всего есть 4 варианта.
        Map<Integer, String> stringMapper;
        Map<Integer, Object> objectMapper = new HashMap<>();
        if (!retainIdentity && inputHasFeatureOfRetainingIdentity(input)) {
            input = transformInputWithIdentityToWithout(input);
            stringMapper = new HashMap<>();
            stringMapper.put(0, input);
        } else if (retainIdentity && inputHasFeatureOfRetainingIdentity(input)) {
            stringMapper = readStringMapperFromLines(input);
        } else {
            // Два варианта:
            // Сохранять индентичность, но при сохранении индентичность не поддрживалась.
            // Не сохранять идентичость, и при сохранении индентичность не поддрживалась.
            stringMapper = new HashMap<>();
            stringMapper.put(0, input);
        }
        // Запуск самой десериализации.
        try {
            readFromStringRecursive(clazz, 0, stringMapper, objectMapper);
        } catch (InstantiationException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Десериализованный объект находится на нулевом месте.
        return (T) objectMapper.get(0);
    }

    private void splitToFieldsNamesAndValues(String string, List<String> fieldNames, List<String> fieldValues) {
        int index = 0;
        int left;
        int right;
        while (index < string.length()) {
            left = string.indexOf('\"', index);
            right = string.indexOf('\"', left + 1);
            fieldNames.add(string.substring(left + 1, right));

            index = right + 3;
            if (string.charAt(index) == '{') {
                right = string.indexOf('}', index) + 1;
            } else if (string.charAt(index) == '[') {
                right = string.indexOf(']', index) + 1;
            } else {
                right = string.indexOf(", \"", index);
                if (right == -1) {
                    right = string.indexOf(" }", index);
                }
            }
            fieldValues.add(string.substring(index, right));
            index = right + 2;
        }
    }

    private Object parseEndpointField(Class<?> fieldType, String fieldValue) {
        // Всего 8 примитивов + String.
        // Признак строки - кавычки.
        if (fieldValue.charAt(0) == '\"') {
            return fieldValue.substring(1, fieldValue.length() - 1);
        } else if (fieldValue.equals("true")) {
            return true;
        } else if (fieldValue.equals("false")) {
            return false;
        } else if (fieldType == byte.class || fieldType == Byte.class) {
            return Byte.parseByte(fieldValue);
        } else if (fieldType == char.class || fieldType == Character.class) {
            return fieldValue.charAt(1);
        } else if (fieldType == short.class || fieldType == Short.class) {
            return Short.parseShort(fieldValue);
        } else if (fieldType == int.class || fieldType == Integer.class) {
            return Integer.parseInt(fieldValue);
        } else if (fieldType == long.class || fieldType == Long.class) {
            return Long.parseLong(fieldValue);
        } else if (fieldType == float.class || fieldType == Float.class) {
            return Float.parseFloat(fieldValue);
        } else if (fieldType == double.class || fieldType == Double.class) {
            return Double.parseDouble(fieldValue);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private <T> void readFromStringRecursive(Class<T> clazz, int index,
                                             Map<Integer, String> stringMapper,
                                             Map<Integer, Object> objectMapper) throws Exception {
        if (!clazz.isAnnotationPresent(Exported.class)) {
            throw new Exception();
        }
        // Вызов конструктора по умолчанию через рефлексию.
        T resultObject = clazz.newInstance();
        List<String> fieldNames = new ArrayList<>();
        List<String> fieldValues = new ArrayList<>();

        // Разделение строки на список переменных и соответствующих им значений.
        splitToFieldsNamesAndValues(stringMapper.get(index), fieldNames, fieldValues);

        // Прохожусь по всем полям в объекте.
        for (int i = 0; i < fieldNames.size(); ++i) {
            Field currField = getDeclaredFieldForReading(clazz, fieldNames.get(i));
            if (currField == null) {
                continue;
            }
            currField.setAccessible(true);

            // 1) Это может быть ссылкой на другой объект.
            if (fieldValues.get(i).charAt(0) == '%') {
                int objectNumber = Integer.parseInt(fieldValues.get(i).substring(1));
                Class<?> objectClass = currField.getType();
                if (!objectMapper.containsKey(objectNumber)) {
                    readFromStringRecursive(objectClass, objectNumber, stringMapper, objectMapper);
                }
                currField.set(resultObject, objectMapper.get(objectNumber));

            } else if (fieldValues.get(i).charAt(0) == '{') {
                // 2) Это может быть просто объектом.
                int objectNumber = stringMapper.size();
                stringMapper.put(objectNumber, fieldValues.get(i));
                readFromStringRecursive(currField.getType(), objectNumber, stringMapper, objectMapper);
                currField.set(resultObject, objectMapper.get(objectNumber));

            } else if (fieldValues.get(i).charAt(0) == '[') {
                var genericType = (Class<?>) ((ParameterizedType) currField.getGenericType()).getActualTypeArguments()[0];
                // 3) Это может быть массив/множество.
                Object collection;// = currField.getClass().newInstance();
                if (List.class.isAssignableFrom(currField.getType())) {
                    collection = ArrayList.class.newInstance();
                } else {
                    collection = HashSet.class.newInstance();
                }
                Method add = Collection.class.getDeclaredMethod("add", Object.class);
                var splitted = fieldValues.get(i).split(", ");
                splitted[0] = splitted[0].replaceFirst("\\[ ", "");
                splitted[splitted.length - 1] = splitted[splitted.length - 1].replaceFirst(" ]", "");
                if (isEndpointClass(genericType)) {
                    for (String elem : splitted) {
                        add.invoke(collection, parseEndpointField(genericType, elem));
                    }
                } else {
                    for (String elem : splitted) {
                        // Не успеваю сделать поддержку обычных объектов.
                        throw new UnsupportedEncodingException();
                    }
                }
                currField.set(resultObject, collection);

            } else {
                // 3) Иначе это примитив, обертка примитива или строка.
                currField.set(resultObject, parseEndpointField(currField.getType(), fieldValues.get(i)));
            }
        }
        // После полной обработки объект записывается в словарь.
        objectMapper.put(index, resultObject);
        // И далее рекурсивные вызовы будут раскручиваться.
    }

    /**
     * Читает объект класса {@code clazz} из {@code InputStream}'а
     * и возвращает восстановленный экземпляр класса {@code clazz}.
     * <p>
     * Данный метод закрывает {@code inputStream}.
     * <p>
     * Пример вызова:
     *
     * <pre>
     * String input = """
     * {"comment":"Хорошая работа","resolved":false}""";
     * ReviewComment reviewComment = mapper.read(ReviewComment.class,
     * new
     * ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
     * System.out.println(reviewComment);
     * </pre>
     *
     * @param clazz       класс, сохранённый экземпляр которого находится в {@code
     *                    inputStream}
     * @param inputStream поток ввода, содержащий строку в {@link
     *                    StandardCharsets#UTF_8} кодировке
     * @return восстановленный экземпляр класса {@code clazz}
     * @throws IOException в случае ошибки ввода-вывода
     */
    @Override
    public <T> T read(Class<T> clazz, InputStream inputStream) throws IOException {
        return null;
    }

    /**
     * Читает сохранённое представление экземпляра класса {@code clazz} из {@code
     * File}'а
     * и возвращает восстановленный экземпляр класса {@code clazz}.
     * <p>
     * Пример вызова:
     *
     * <pre>
     * ReviewComment reviewComment = mapper.read(ReviewComment.class, new
     * File("/tmp/review"));
     * System.out.println(reviewComment);
     * </pre>
     *
     * @param clazz класс, сохранённый экземпляр которого находится в файле
     * @param file  файл, содержимое которого - строковое представление экземпляра
     *              {@code clazz}
     *              в {@link StandardCharsets#UTF_8} кодировке
     * @return восстановленный экземпляр {@code clazz}
     * @throws IOException в случае ошибки ввода-вывода
     */
    @Override
    public <T> T read(Class<T> clazz, File file) throws IOException {
        return null;
    }

    private boolean isEndpointClass(Class<?> type) {
        return type.isPrimitive() || type == Double.class || type == Float.class ||
                type == Long.class || type == Integer.class || type == Short.class ||
                type == Character.class || type == Byte.class || type == Boolean.class ||
                type == String.class;
    }

    private String getEndpointObjectString(Object object) {
        if (object instanceof String) {
            return "\"" + object + "\"";
        }
        return object.toString();
    }

    private String writeToStringRecursive(boolean retainIdentity, Object object,
                                          IdentityHashMap<Object, Integer> identityHashMap,
                                          Map<Integer, String> resultIdMapper)
            throws Exception {
        Class<?> objectClass = object.getClass();
        if (object instanceof List<?> || object instanceof Set<?>) {
            StringBuilder sb = new StringBuilder("[ ");
            for (Object elem : (Collection<?>) object) {
                sb.append(writeToStringRecursive(retainIdentity, elem, identityHashMap, resultIdMapper)).append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append(" ]");
            return sb.toString();
        }
        if (isEndpointClass(objectClass)) {
            return getEndpointObjectString(object);
        }
        if (!objectClass.isAnnotationPresent(Exported.class)) {
            throw new Exception();
        }

        if (retainIdentity && identityHashMap.containsKey(object)) {
            return "%" + identityHashMap.get(object);
        } else {
            // Если в массиве identity есть ссылка на этот объект, значит, произошел цикл.
            if (identityHashMap.containsKey(object)) {
                throw new Exception("Цикл!");
            }
            identityHashMap.put(object, identityHashMap.size());

            StringBuilder result = new StringBuilder("{ ");
            List<String> fieldStringsToJoin = new ArrayList<>();
            Field[] fields = objectClass.getDeclaredFields();
            try {
                for (Field field : fields) {
                    String fieldName = getDeclaredFieldForWriting(field);
                    if (fieldName == null) {
                        continue;
                    }
                    field.setAccessible(true);
                    if (field.get(object) == null) {
                        if (objectClass.getAnnotation(Exported.class).nullHandling() == NullHandling.INCLUDE) {
                            fieldStringsToJoin.add("\"" + field.getName() + "\": null");
                        }
                    } else {
                        fieldStringsToJoin.add("\"" + fieldName + "\": " +
                                writeToStringRecursive(
                                        retainIdentity, field.get(object), identityHashMap, resultIdMapper));
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            result.append(String.join(", ", fieldStringsToJoin));
            result.append(" }");

            if (retainIdentity) {
                resultIdMapper.put(identityHashMap.get(object), result.toString());
                return "%" + identityHashMap.get(object);
            } else {
                return result.toString();
            }
        }
    }

    /**
     * Сохраняет {@code object} в строку
     * <p>
     * Пример вызова:
     *
     * <pre>
     * ReviewComment reviewComment = new ReviewComment();
     * reviewComment.setComment("Хорошая работа");
     * reviewComment.setResolved(false);
     *
     * String string = mapper.writeToString(reviewComment);
     * System.out.println(string);
     * </pre>
     *
     * @param object объект для сохранения
     * @return строковое представление объекта в выбранном формате
     */
    @Override
    public String writeToString(Object object) throws Exception {
        Map<Integer, String> mapper = new HashMap<>();
        StringBuilder result = new StringBuilder(
                writeToStringRecursive(retainIdentity, object, new IdentityHashMap<>(), mapper));
        for (var pair : mapper.entrySet()) {
            result.append("\n").append(pair.getKey()).append(": ").append(pair.getValue());
        }
        return result.toString();
    }

    /**
     * Сохраняет {@code object} в {@link OutputStream}.
     * <p>
     * То есть после вызова этого метода в {@link OutputStream} должны оказаться
     * байты, соответствующие строковому
     * представлению {@code object}'а в кодировке {@link
     * StandardCharsets#UTF_8}
     * <p>
     * Данный метод закрывает {@code outputStream}
     * <p>
     * Пример вызова:
     *
     * <pre>
     * ReviewComment reviewComment = new ReviewComment();
     * reviewComment.setComment("Хорошая работа");
     * reviewComment.setResolved(false);
     *
     * mapper.write(reviewComment, new FileOutputStream("/tmp/review"));
     * </pre>
     *
     * @param object       объект для сохранения
     * @param outputStream
     * @throws IOException в случае ошибки ввода-вывода
     */
    @Override
    public void write(Object object, OutputStream outputStream) throws IOException {

    }

    /**
     * Сохраняет {@code object} в {@link File}.
     * <p>
     * То есть после вызова этого метода в {@link File} должны оказаться байты,
     * соответствующие строковому
     * представлению {@code object}'а в кодировке {@link
     * StandardCharsets#UTF_8}
     * <p>
     * Данный метод закрывает {@code outputStream}
     * <p>
     * Пример вызова:
     *
     * <pre>
     * ReviewComment reviewComment = new ReviewComment();
     * reviewComment.setComment("Хорошая работа");
     * reviewComment.setResolved(false);
     *
     * mapper.write(reviewComment, new File("/tmp/review"));
     * </pre>
     *
     * @param object объект для сохранения
     * @param file
     * @throws IOException в случае ошибки ввода-вывода
     */
    @Override
    public void write(Object object, File file) throws IOException {

    }
}
