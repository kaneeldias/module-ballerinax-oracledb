/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.stdlib.oracledb.utils;

import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.StructureType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.oracledb.Constants;
import io.ballerina.stdlib.sql.exception.ApplicationError;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;


import static io.ballerina.runtime.api.utils.StringUtils.fromString;

/**
 * This class converts ballerina custom types to driver specific objects.
 *
 * @since 0.1.0
 */
public class ConverterUtils {
    private static final ArrayType byteArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_BYTE);
    private static final ArrayType stringArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_STRING);
    private static final ArrayType booleanArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_BOOLEAN);
    private static final ArrayType intArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_INT);
    private static final ArrayType floatArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_FLOAT);
    private static final ArrayType decimalArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_DECIMAL);

    /**
     * Convert IntervalYearToMonthValue value to String.
     * @param value Custom IntervalYearToMonthValue value
     * @return String of INTERVAL_YEAR_TO_MONTH
     * @throws ApplicationError error thrown if invalid types are passed
     */
    public static String convertIntervalYearToMonth(Object value)
            throws ApplicationError {
        Map<String, Object> fields = getRecordData(value, Constants.Types.OracleDbTypes.INTERVAL_YEAR_TO_MONTH);
        Object yearObject = fields.get(Constants.Types.IntervalYearToMonth.YEARS);
        Object monthObject = fields.get(Constants.Types.IntervalYearToMonth.MONTHS);
        String year = getIntervalString(yearObject, Constants.Types.OracleDbTypes.INTERVAL_YEAR_TO_MONTH);
        String month = getIntervalString(monthObject, Constants.Types.OracleDbTypes.INTERVAL_YEAR_TO_MONTH);
        return year + "-" + month;
    }

    /**
     * Convert IntervalDayToSecondValue value to String.
     * @param value Custom IntervalDayToSecond value
     * @return String of INTERVAL_DAY_TO_SECOND
     * @throws ApplicationError error thrown if invalid types are passed
     */
    public static String convertIntervalDayToSecond(Object value)
            throws ApplicationError {
        Map<String, Object> fields = getRecordData(value, Constants.Types.OracleDbTypes.INTERVAL_DAY_TO_SECOND);
        Object dayObject = fields.get(Constants.Types.IntervalDayToSecond.DAYS);
        Object hourObject = fields.get(Constants.Types.IntervalDayToSecond.HOURS);
        Object minuteObject = fields.get(Constants.Types.IntervalDayToSecond.MINUTES);
        Object secondObject = fields.get(Constants.Types.IntervalDayToSecond.SECONDS);
        String day = getIntervalString(dayObject, Constants.Types.OracleDbTypes.INTERVAL_DAY_TO_SECOND);
        String hour = getIntervalString(hourObject, Constants.Types.OracleDbTypes.INTERVAL_DAY_TO_SECOND);
        String minute = getIntervalString(minuteObject, Constants.Types.OracleDbTypes.INTERVAL_DAY_TO_SECOND);
        String second = getIntervalString(secondObject, Constants.Types.OracleDbTypes.INTERVAL_DAY_TO_SECOND);

        return day + " " + hour + ":" + minute + ":" + second;
    }

    /**
     * Convert OracleObjectValue value to oracle.sql.STRUCT.
     * @param value Custom Bfile value
     * @return String of BFILE
     */
    public static Struct convertOracleObject(Connection connection, Object value)
            throws ApplicationError, SQLException {
        Map<String, Object> fields = getRecordData(value, Constants.Types.OracleDbTypes.OBJECT_TYPE);
        String objectTypeName = ((BString) fields.get(Constants.Types.OracleObject.TYPE_NAME))
                .getValue().toUpperCase(Locale.ENGLISH);
        Object[] attributes = (Object[]) fields.get(Constants.Types.OracleObject.ATTRIBUTES);
        try {
            return connection.createStruct(objectTypeName, attributes);
        } catch (SQLException e) {
            throw(e);
        } catch (Exception e) {
            throw new ApplicationError("The array contains elements of unmappable types.");
        }
    }

    /**
     * Convert VArray value to oracle.sql.Array.
     * @param value Custom VArray Value
     * @return sql Array
     * @throws ApplicationError throws error if the parameter types are incorrect
     */
    public static Array convertVarray(Connection connection, Object value)
            throws ApplicationError, SQLException {
        Map<String, Object> fields = getRecordData(value, Constants.Types.OracleDbTypes.VARRAY);
        String name = ((BString) fields.get(Constants.Types.Varray.NAME)).getValue().toUpperCase(Locale.ENGLISH);
        Object varray = fields.get(Constants.Types.Varray.ELEMENTS);
        return Utils.getOracleConnection(connection).createARRAY(name, varray);
    }

    /**
     * Convert SQL array to string array.
     * @param dataArray SQL array retrieved from the database
     * @return BArray of type string generated from the SQL array
     * @throws ApplicationError throws error if the parameter types are incorrect
     */
    public static BArray convertToStringArrayFromVarray(Object[] dataArray)
            throws ApplicationError {
        if (dataArray[0] instanceof String) {
            BArray typedArray = ValueCreator.createArrayValue(stringArrayType);
            int length = dataArray.length;
            for (int i = 0; i < length; ++i) {
                typedArray.add(i, StringUtils.fromString((String) dataArray[i]));
            }
            return typedArray;
        } else {
            throw Utils.throwArrayTypeCastError(Constants.Types.BallerinaArrayTypes.STRING);
        }

    }

    /**
     * Convert SQL array to int array.
     * @param dataArray SQL array retrieved from the database
     * @return BArray of type string generated from the SQL array
     * @throws ApplicationError throws error if the parameter types are incorrect
     */
    public static BArray convertToIntArrayFromVarray(Object[] dataArray)
            throws ApplicationError {
        BArray typedArray = ValueCreator.createArrayValue(intArrayType);
        int length = dataArray.length;
        if (dataArray[0] instanceof BigDecimal) {
            for (int i = 0; i < length; ++i) {
                typedArray.add(i, ((BigDecimal) dataArray[i]).longValue());
            }
        } else {
            throw Utils.throwArrayTypeCastError(Constants.Types.BallerinaArrayTypes.INT);
        }
        return typedArray;
    }

    /**
     * Convert SQL array to float array.
     * @param dataArray SQL array retrieved from the database
     * @return BArray of type float generated from the SQL array
     * @throws ApplicationError throws error if the parameter types are incorrect
     */
    public static BArray convertToFloatArrayFromVarray(Object[] dataArray)
            throws ApplicationError {
        BArray typedArray = ValueCreator.createArrayValue(floatArrayType);
        int length = dataArray.length;
        if (dataArray[0] instanceof BigDecimal) {
            for (int i = 0; i < length; ++i) {
                typedArray.add(i, ((BigDecimal) dataArray[i]).doubleValue());
            }
        } else {
            throw Utils.throwArrayTypeCastError(Constants.Types.BallerinaArrayTypes.FLOAT);
        }
        return typedArray;
    }

    /**
     * Convert SQL array to decimal array.
     * @param dataArray SQL array retrieved from the database
     * @return BArray of type decimal generated from the SQL array
     * @throws ApplicationError throws error if the parameter types are incorrect
     */
    public static BArray convertToDecimalArrayFromVarray(Object[] dataArray)
            throws ApplicationError {
        BArray typedArray = ValueCreator.createArrayValue(decimalArrayType);
        int length = dataArray.length;
        if (dataArray[0] instanceof BigDecimal) {
            for (int i = 0; i < length; ++i) {
                typedArray.add(i, ValueCreator.createDecimalValue((BigDecimal) dataArray[i]));
            }
        } else {
            throw Utils.throwArrayTypeCastError(Constants.Types.BallerinaArrayTypes.DECIMAL);
        }
        return typedArray;
    }

    /**
     * Convert SQL array to boolean array.
     * @param dataArray SQL array retrieved from the database
     * @return BArray of type string generated from the SQL array
     * @throws ApplicationError throws error if the parameter types are incorrect
     */
    public static BArray convertToBooleanArrayFromVarray(Object[] dataArray)
            throws ApplicationError {
        BArray typedArray = ValueCreator.createArrayValue(booleanArrayType);
        int length = dataArray.length;
        if (dataArray[0] instanceof BigDecimal) {
            for (int i = 0; i < length; ++i) {
                Boolean element = dataArray[i].equals(BigDecimal.ONE);
                typedArray.add(i, element);
            }
        } else {
            throw Utils.throwArrayTypeCastError(Constants.Types.BallerinaArrayTypes.BOOLEAN);
        }
        return typedArray;
    }

    /**
     * Convert SQL array to string array.
     * @param dataArray SQL array retrieved from the database
     * @return BArray of type string generated from the SQL array
     * @throws ApplicationError throws error if the parameter types are incorrect
     */
    public static BArray convertToByteArrayFromVarray(Object[] dataArray)
            throws ApplicationError {
        BArray typedArray = ValueCreator.createArrayValue(byteArrayType);
        if (dataArray[0].getClass() == byte[].class) {
            byte[] byteArray = (byte[]) dataArray[0];
            int length = byteArray.length;
            for (int i = 0; i < length; ++i) {
                typedArray.add(i, byteArray[i]);
            }
        } else {
            throw Utils.throwArrayTypeCastError(Constants.Types.BallerinaArrayTypes.BYTE);
        }
        return typedArray;
    }

    private static String getIntervalString(Object param, String typeName) throws ApplicationError {
        String value;
        if (param instanceof BString) {
            value = ((BString) param).getValue();
        } else if (param instanceof Long || param instanceof Double) {
            value = param.toString();
        } else if (param instanceof BDecimal) {
            value = Double.toString(((BDecimal) param).floatValue());
        } else {
            throw Utils.throwInvalidParameterError(param, typeName);
        }
        return value;
    }

    private static Map<String, Object> getRecordData(Object value, String sqlType)
            throws ApplicationError {
        Type type = TypeUtils.getType(value);
        if (type.getTag() != TypeTags.RECORD_TYPE_TAG) {
            throw Utils.throwInvalidParameterError(value, sqlType);
        }
        Map<String, Field> structFields = ((StructureType) type).getFields();
        int fieldCount = structFields.size();
        Iterator<Field> fieldIterator = structFields.values().iterator();
        HashMap<String, Object> structData = new HashMap<>();
        for (int i = 0; i < fieldCount; i++) {
            Field field = fieldIterator.next();
            Object bValue = ((BMap) value).get(fromString(field.getFieldName()));
            int typeTag = field.getFieldType().getTag();
            switch (typeTag) {
                case TypeTags.INT_TAG:
                case TypeTags.FLOAT_TAG:
                case TypeTags.STRING_TAG:
                case TypeTags.BOOLEAN_TAG:
                case TypeTags.DECIMAL_TAG:
                    structData.put(field.getFieldName(), bValue);
                    break;
                case TypeTags.ARRAY_TAG:
                    Object arrayData = getArrayData(bValue);
                    structData.put(field.getFieldName(), arrayData);
                    break;
                case TypeTags.RECORD_TYPE_TAG:
                    structData.put(field.getFieldName(), getRecordData(bValue, sqlType));
                    break;
                case TypeTags.UNION_TAG:
                    if (bValue == null) {
                        structData.put(field.getFieldName(), null);
                    } else if (bValue instanceof BArray) {
                        structData.put(field.getFieldName(), getArrayData(bValue));
                    } else if (bValue instanceof BString) {
                        structData.put(field.getFieldName(), bValue);
                    } else {
                        throw Utils.throwInvalidParameterError(value, sqlType);
                    }
                    break;
                default:
                    throw Utils.throwInvalidParameterError(value, sqlType);
            }
        }
        return structData;
    }

    private static Object[] getArrayData(Object bValue) throws ApplicationError {
        Type elementType = ((BArray) bValue).getElementType();
        int tag = elementType.getTag();
        switch (tag) {
            case TypeTags.BYTE_TAG:
                return getByteArrayData(bValue);
            case TypeTags.INT_TAG:
                return getIntArrayData(bValue);
            case TypeTags.BOOLEAN_TAG:
                return getBooleanArrayData(bValue);
            case TypeTags.FLOAT_TAG:
                return getFloatArrayData(bValue);
            case TypeTags.DECIMAL_TAG:
                return getDecimalArrayData(bValue);
            case TypeTags.STRING_TAG:
                return getStringArrayData(bValue);
            case TypeTags.ANYDATA_TAG:
                return getAnydataArrayData(bValue);
            default:
                throw new ApplicationError("Unsupported data type for array specified for struct parameter");
        }
    }

    private static Object[] getByteArrayData(Object value) {
        return new byte[][]{((BArray) value).getBytes()};
    }

    private static Object[] getIntArrayData(Object value) {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new Long[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            arrayData[i] = ((BArray) value).getInt(i);
        }
        return arrayData;
    }

    private static Object[] getFloatArrayData(Object value) {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new Double[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            arrayData[i] = ((BArray) value).getFloat(i);
        }
        return arrayData;
    }

    private static Object[] getStringArrayData(Object value) {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new String[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            arrayData[i] = ((BArray) value).getBString(i).getValue();
        }
        return arrayData;
    }

    private static Object[] getBooleanArrayData(Object value) {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new Boolean[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            arrayData[i] = ((BArray) value).getBoolean(i);
        }
        return arrayData;
    }

    private static Object[] getDecimalArrayData(Object value) {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new BigDecimal[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            arrayData[i] = ((BDecimal) ((BArray) value).getRefValue(i)).value();
        }
        return arrayData;
    }

    private static Object[] getAnydataArrayData(Object value) throws ApplicationError {
        int arrayLength = ((BArray) value).size();
        Object[] arrayData = new Object[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
            Object element = ((BArray) value).getRefValue(i);
            if (element instanceof Double || element instanceof Long || element == null) {
                arrayData[i] = element;
            } else if (element instanceof BString) {
                arrayData[i] = ((BString) element).getValue();
            } else if (element instanceof BDecimal) {
                arrayData[i] = ((BDecimal) element).decimalValue();
            } else if (element instanceof BArray) {
                arrayData[i] = getAnydataArrayData(element);
            } else {
                throw new ApplicationError("The array contains elements of unmappable types.");
            }
        }
        return arrayData;
    }
}
