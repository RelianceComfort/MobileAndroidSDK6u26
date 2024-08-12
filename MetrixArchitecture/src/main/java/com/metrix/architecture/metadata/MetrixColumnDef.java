package com.metrix.architecture.metadata;

import com.metrix.architecture.constants.MetrixControlCase;
import com.metrix.architecture.utilities.AndroidResourceHelper;
import com.metrix.architecture.utilities.SpinnerKeyValuePair;

import java.lang.reflect.Type;
import java.text.Format;
import java.util.ArrayList;

/**
 * Contains the meta data describing a database column that should be bound to
 * the current layout along with instructions as to how it's data should be
 * formatted, it's view should be constructed (for example, if the data is to be
 * displayed in a spinner, this defines how the spinner should be populated) and
 * where or not it's part of the primary key or is required.
 * 
 * @since 5.4
 */
public class MetrixColumnDef {

	/**
	 * The R.id of the view this column should be bound to.
	 */
	public Integer id;
	
	/**
	 * The R.id of the view this column's label should be bound to.
	 */
	public Integer labelId;
	
	/**
	 * The unique identifier of the corresponding field metadata.
	 */
	public Integer fieldId;

	/**
	 * The name of the datbase column.
	 */
	public String columnName;

	/**
	 * The data type of the data contained in the column.
	 */
	public Type dataType;

	/**
	 * TRUE if this column is part of the primary key, FALSE otherwise.
	 */
	public boolean primaryKey = false;

	/**
	 * TRUE if this column requires a value, FALSE otherwise.
	 */
	public boolean required = false;

	/**
	 * Defines if the value should be forced upper or lower case.
	 */
	public MetrixControlCase forceCase = MetrixControlCase.NONE;

	/**
	 * Contains a Regular Expression that should be used to validate the data.
	 */
	public String validation;

	/**
	 * If this column is using a spinner, defines how the spinner should be
	 * populated.
	 */
	public MetrixDropDownDef lookupDef;

	/**
	 * If this column is using a spinner, contains the values to be added to the
	 * spinner.
	 */
	public ArrayList<SpinnerKeyValuePair> lookupValues;

	/**
	 * A friendly name to be used in informational or error messages to the
	 * user.
	 */
	public String friendlyName;

	/**
	 * An instance of a formatter that should be applied to the value of the
	 * column as part of the initial data binding to the layout.
	 */
	public Format formatter;
	
	/**
	 * The maximum number of characters that can be displayed on the activity screen. 
	 * The property will be used as part of the initial data binding to the layout.
	 */
	public int maximumCharacters = 0;

	/**
	 * The control type of the column.
	 */
	public String controlType;

	// Attachment Field items below
	public boolean readOnlyInMetadata = false;
	public boolean allowPhoto = false;
	public boolean allowVideo = false;
	public boolean allowFile = false;
	public int cardScreenID = 0;
	public String transactionIdTableName = "";
	public String transactionIdColumnName = "";

	// Signatur Field items below
	public boolean allowClear = false;
	public boolean readOnly = false;
	public boolean visible = false;
	public String signerColumn = "";
	public String messageId = "";

	/**
	 * Base constructor.
	 */
	public MetrixColumnDef() {
	}

	/**
	 * A convenience constructor. It's recommended that you instead use an
	 * overload that includes a friendly name.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * 
	 * @deprecated As of release 5.5.1, use an overload that includes a friendly
	 *             name instead.
	 */
	@Deprecated
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.dataType = dataType;
	}

	/**
	 * A convenience constructor. It's recommended that you instead use an
	 * overload that includes a friendly name.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * 
	 * @deprecated As of release 5.5.1, use an overload that includes a friendly
	 *             name instead.
	 */
	@Deprecated
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, boolean primaryKey) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.dataType = dataType;
		this.primaryKey = primaryKey;
	}

	/**
	 * A convenience constructor. It's recommended that you instead use an
	 * overload that includes a friendly name.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param formatter
	 *            An instance of a formatter that should be applied to the value
	 *            of the column as part of the initial data binding to the
	 *            layout.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * 
	 * @deprecated As of release 5.5.1, use an overload that includes a friendly
	 *             name instead.
	 */
	@Deprecated
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, Format formatter, boolean primaryKey) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.dataType = dataType;
		this.formatter = formatter;
		this.primaryKey = primaryKey;
	}

	/**
	 * A convenience constructor. It's recommended that you instead use an
	 * overload that includes a friendly name.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * 
	 * @deprecated As of release 5.5.1, use an overload that includes a friendly
	 *             name instead.
	 */
	@Deprecated
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, MetrixControlCase forceCase) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.forceCase = forceCase;
		this.dataType = dataType;
	}

	/**
	 * A convenience constructor. It's recommended that you instead use an
	 * overload that includes a friendly name.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @deprecated As of release 5.5.1, use an overload that includes a friendly
	 *             name instead.
	 */
	@Deprecated
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, boolean primaryKey, MetrixControlCase forceCase) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.forceCase = forceCase;
		this.primaryKey = primaryKey;
		this.dataType = dataType;
	}

	/**
	 * A convenience constructor. It's recommended that you instead use an
	 * overload that includes a friendly name.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * @deprecated As of release 5.5.1, use an overload that includes a friendly
	 *             name instead.
	 */
	@Deprecated
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, MetrixControlCase forceCase, String validation) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.forceCase = forceCase;
		this.validation = validation;
		this.dataType = dataType;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * 
	 *            <pre>
	 * MetrixTableDef taskDef = new MetrixTableDef(&quot;task&quot;,
	 * 		MetrixTransactionTypes.UPDATE);
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__metrix_row_id,
	 * 		&quot;metrix_row_id&quot;, true, double.class, &quot;Metrix Row ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_id, &quot;task_id&quot;, true,
	 * 		double.class, true, &quot;Task ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_status, &quot;task_status&quot;,
	 * 		true, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;task_status&quot;,
	 * 		&quot;task_status&quot;, &quot;description&quot;, &quot;&quot;, &quot;&quot;, &quot;Task Status&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_type, &quot;task_type&quot;,
	 * 		false, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;TASK_TYPE&quot;, &quot;Task Type&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__priority, &quot;priority&quot;, false,
	 * 		String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;PRIORITY&quot;, &quot;Priority&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__plan_start_dttm,
	 * 		&quot;plan_start_dttm&quot;, true, Date.class, &quot;Planned Start&quot;));
	 * </pre>
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.dataType = dataType;
		this.friendlyName = friendlyName;
	}
	
	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * @param maximumChars
	 * 			  Set limitation for number of displayed characters  	
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, String friendlyName, int maximumChars) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.dataType = dataType;
		this.friendlyName = friendlyName;
		this.maximumCharacters = maximumChars;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param formatter
	 *            An instance of Java.Text.Format to apply to the column's value
	 *            when binding to the layout.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, Format formatter, String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.dataType = dataType;
		this.formatter = formatter;
		this.friendlyName = friendlyName;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param formatter
	 *            An instance of Java.Text.Format to apply to the column's value
	 *            when binding to the layout.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * @param maximumChars
	 * 			  Set limitation for number of displayed characters  	            
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, Format formatter, String friendlyName, int maximumChars) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.dataType = dataType;
		this.formatter = formatter;
		this.friendlyName = friendlyName;
		this.maximumCharacters = maximumChars;
	}	
	
	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param lookupValues
	 *            The values to populate the spinner with.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, ArrayList<SpinnerKeyValuePair> lookupValues,
			String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.dataType = dataType;
		this.lookupValues = lookupValues;
		this.friendlyName = friendlyName;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param formatter
	 *            An instance of Java.Text.Format to apply to the column's value
	 *            when binding to the layout.
	 * @param lookupValues
	 *            The values to populate the spinner with.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 */	
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, Format formatter, ArrayList<SpinnerKeyValuePair> lookupValues,
			String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.dataType = dataType;
		this.formatter = formatter;
		this.lookupValues = lookupValues;
		this.friendlyName = friendlyName;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * 
	 *            <pre>
	 * MetrixTableDef taskDef = new MetrixTableDef(&quot;task&quot;,
	 * 		MetrixTransactionTypes.UPDATE);
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__metrix_row_id,
	 * 		&quot;metrix_row_id&quot;, true, double.class, &quot;Metrix Row ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_id, &quot;task_id&quot;, true,
	 * 		double.class, true, &quot;Task ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_status, &quot;task_status&quot;,
	 * 		true, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;task_status&quot;,
	 * 		&quot;task_status&quot;, &quot;description&quot;, &quot;&quot;, &quot;&quot;, &quot;Task Status&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_type, &quot;task_type&quot;,
	 * 		false, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;TASK_TYPE&quot;, &quot;Task Type&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__priority, &quot;priority&quot;, false,
	 * 		String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;PRIORITY&quot;, &quot;Priority&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__plan_start_dttm,
	 * 		&quot;plan_start_dttm&quot;, true, Date.class, &quot;Planned Start&quot;));
	 * </pre>
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, boolean primaryKey, String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.primaryKey = primaryKey;
		this.dataType = dataType;
		this.friendlyName = friendlyName;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * @param maximumChars
	 * 			  Set limitation for number of displayed characters  	            
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, boolean primaryKey, String friendlyName, int maximumChars) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.primaryKey = primaryKey;
		this.dataType = dataType;
		this.friendlyName = friendlyName;
		this.maximumCharacters = maximumChars;
	}	
	
	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param formatter
	 *            An instance of Java.Text.Format to apply to the column's value
	 *            when binding to the layout.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 */ 
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, Format formatter, boolean primaryKey, String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.primaryKey = primaryKey;
		this.dataType = dataType;
		this.formatter = formatter;
		this.friendlyName = friendlyName;
	}
	
	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param formatter
	 *            An instance of Java.Text.Format to apply to the column's value
	 *            when binding to the layout.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * @param maximumChars
	 * 			  Set limitation for number of displayed characters  	              
	 */ 
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, Format formatter, boolean primaryKey, String friendlyName, int maximumChars) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.primaryKey = primaryKey;
		this.dataType = dataType;
		this.formatter = formatter;
		this.friendlyName = friendlyName;
		this.maximumCharacters = maximumChars;
	}
	

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * 
	 *            <pre>
	 * MetrixTableDef taskDef = new MetrixTableDef(&quot;task&quot;,
	 * 		MetrixTransactionTypes.UPDATE);
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__metrix_row_id,
	 * 		&quot;metrix_row_id&quot;, true, double.class, &quot;Metrix Row ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_id, &quot;task_id&quot;, true,
	 * 		double.class, true, &quot;Task ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_status, &quot;task_status&quot;,
	 * 		true, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;task_status&quot;,
	 * 		&quot;task_status&quot;, &quot;description&quot;, &quot;&quot;, &quot;&quot;, &quot;Task Status&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_type, &quot;task_type&quot;,
	 * 		false, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;TASK_TYPE&quot;, &quot;Task Type&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__priority, &quot;priority&quot;, false,
	 * 		String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;PRIORITY&quot;, &quot;Priority&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__plan_start_dttm,
	 * 		&quot;plan_start_dttm&quot;, true, Date.class, &quot;Planned Start&quot;));
	 * </pre>
	 * @deprecated As of release 5.5.1, use an overload that includes a friendly
	 *             name instead.
	 */
	@Deprecated
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, boolean primaryKey, MetrixControlCase forceCase,
			String validation) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.primaryKey = primaryKey;
		this.forceCase = forceCase;
		this.validation = validation;
		this.dataType = dataType;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * 
	 * 
	 *            <pre>
	 * MetrixTableDef taskDef = new MetrixTableDef(&quot;task&quot;,
	 * 		MetrixTransactionTypes.UPDATE);
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__metrix_row_id,
	 * 		&quot;metrix_row_id&quot;, true, double.class, &quot;Metrix Row ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_id, &quot;task_id&quot;, true,
	 * 		double.class, true, &quot;Task ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_status, &quot;task_status&quot;,
	 * 		true, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;task_status&quot;,
	 * 		&quot;task_status&quot;, &quot;description&quot;, &quot;&quot;, &quot;&quot;, &quot;Task Status&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_type, &quot;task_type&quot;,
	 * 		false, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;TASK_TYPE&quot;, &quot;Task Type&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__priority, &quot;priority&quot;, false,
	 * 		String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;PRIORITY&quot;, &quot;Priority&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__plan_start_dttm,
	 * 		&quot;plan_start_dttm&quot;, true, Date.class, &quot;Planned Start&quot;));
	 * </pre>
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, MetrixControlCase forceCase, String validation,
			String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.forceCase = forceCase;
		this.validation = validation;
		this.friendlyName = friendlyName;
		this.dataType = dataType;
	}
	
	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * @param maximumChars
	 * 			  Set limitation for number of displayed characters  
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, MetrixControlCase forceCase, String validation,
			String friendlyName, int maximumChars) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.forceCase = forceCase;
		this.validation = validation;
		this.friendlyName = friendlyName;
		this.dataType = dataType;
		this.maximumCharacters = maximumChars;
	}	

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param formatter
	 *            An instance of Java.Text.Format to apply to the column's value
	 *            when binding to the layout.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 */ 
	 public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, Format formatter, MetrixControlCase forceCase, String validation,
			String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.forceCase = forceCase;
		this.validation = validation;
		this.friendlyName = friendlyName;
		this.dataType = dataType;
		this.formatter = formatter;
	}
	 
		/**
		 * A convenience constructor.
		 * 
		 * @param id
		 *            The R.id of the view this column should be bound to.
		 * @param columnName
		 *            The name of the database column.
		 * @param required
		 *            TRUE if the column requires a value, FALSE otherwise.
		 * @param dataType
		 *            The datatype of the column's data.
		 * @param formatter
		 *            An instance of Java.Text.Format to apply to the column's value
		 *            when binding to the layout.
		 * @param forceCase
		 *            MetrixControlCase defining if the value should be upper or
		 *            lower cased.
		 * @param validation
		 *            Regular expression that should be used to validate the value.
		 * @param friendlyName
		 *            The name to use for error or informational messages describing
		 *            this column.
		 * @param maximumChars
		 * 			  Set limitation for number of displayed characters  
		 */
		 public MetrixColumnDef(Integer id, String columnName, boolean required,
				Type dataType, Format formatter, MetrixControlCase forceCase, String validation,
				String friendlyName, int maximumChars) {
			this.id = id;
			this.columnName = columnName;
			this.required = required;
			this.forceCase = forceCase;
			this.validation = validation;
			this.friendlyName = friendlyName;
			this.dataType = dataType;
			this.formatter = formatter;
			this.maximumCharacters = maximumChars;
		}	 

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * 
	 *            <pre>
	 * MetrixTableDef taskDef = new MetrixTableDef(&quot;task&quot;,
	 * 		MetrixTransactionTypes.UPDATE);
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__metrix_row_id,
	 * 		&quot;metrix_row_id&quot;, true, double.class, &quot;Metrix Row ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_id, &quot;task_id&quot;, true,
	 * 		double.class, true, &quot;Task ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_status, &quot;task_status&quot;,
	 * 		true, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;task_status&quot;,
	 * 		&quot;task_status&quot;, &quot;description&quot;, &quot;&quot;, &quot;&quot;, &quot;Task Status&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_type, &quot;task_type&quot;,
	 * 		false, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;TASK_TYPE&quot;, &quot;Task Type&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__priority, &quot;priority&quot;, false,
	 * 		String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;PRIORITY&quot;, &quot;Priority&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__plan_start_dttm,
	 * 		&quot;plan_start_dttm&quot;, true, Date.class, &quot;Planned Start&quot;));
	 * </pre>
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, boolean primaryKey, MetrixControlCase forceCase,
			String validation, String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.primaryKey = primaryKey;
		this.forceCase = forceCase;
		this.validation = validation;
		this.friendlyName = friendlyName;
		this.dataType = dataType;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * @param maximumChars
	 * 			  Set limitation for number of displayed characters  
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, boolean primaryKey, MetrixControlCase forceCase,
			String validation, String friendlyName, int maximumChars) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.primaryKey = primaryKey;
		this.forceCase = forceCase;
		this.validation = validation;
		this.friendlyName = friendlyName;
		this.dataType = dataType;
		this.maximumCharacters = maximumChars;
	}
	
	
	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param formatter
	 *            An instance of Java.Text.Format to apply to the column's value
	 *            when binding to the layout.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 */
	 public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, Format formatter, boolean primaryKey, MetrixControlCase forceCase,
			String validation, String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.primaryKey = primaryKey;
		this.forceCase = forceCase;
		this.validation = validation;
		this.friendlyName = friendlyName;
		this.dataType = dataType;
		this.formatter = formatter;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param formatter
	 *            An instance of Java.Text.Format to apply to the column's value
	 *            when binding to the layout.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * @param maximumChars
	 * 			  Set limitation for number of displayed characters  
	 */            		 
	 public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, Format formatter, boolean primaryKey, MetrixControlCase forceCase,
			String validation, String friendlyName, int maximumChars) {
		this.id = id;
		this.columnName = columnName;
		this.required = required;
		this.primaryKey = primaryKey;
		this.forceCase = forceCase;
		this.validation = validation;
		this.friendlyName = friendlyName;
		this.dataType = dataType;
		this.formatter = formatter;
		this.maximumCharacters = maximumChars;
	}
 	 
	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param lookupTable
	 *            The name of the table containing data the spinner should be
	 *            populated with.
	 * @param lookupDisplayColumn
	 *            The column that should be displayed by the spinner in it's
	 *            display/value pair.
	 * @param lookupValueColumn
	 *            The value that should be used by the spinner in it's
	 *            display/value pair.
	 * @param lookupFilterColumn
	 *            A column to filter the data being used to populate the
	 *            spinner.
	 * @param lookupFilter
	 *            Data to be used when building the filter to populate the
	 *            spinner.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * 
	 *            <pre>
	 * MetrixTableDef taskDef = new MetrixTableDef(&quot;task&quot;,
	 * 		MetrixTransactionTypes.UPDATE);
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__metrix_row_id,
	 * 		&quot;metrix_row_id&quot;, true, double.class, &quot;Metrix Row ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_id, &quot;task_id&quot;, true,
	 * 		double.class, true, &quot;Task ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_status, &quot;task_status&quot;,
	 * 		true, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;task_status&quot;,
	 * 		&quot;task_status&quot;, &quot;description&quot;, &quot;&quot;, &quot;&quot;, &quot;Task Status&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_type, &quot;task_type&quot;,
	 * 		false, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;TASK_TYPE&quot;, &quot;Task Type&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__priority, &quot;priority&quot;, false,
	 * 		String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;PRIORITY&quot;, &quot;Priority&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__plan_start_dttm,
	 * 		&quot;plan_start_dttm&quot;, true, Date.class, &quot;Planned Start&quot;));
	 * </pre>
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, String lookupTable, String lookupDisplayColumn,
			String lookupValueColumn, String lookupFilterColumn,
			String lookupFilter, String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.dataType = dataType;
		this.required = required;
		this.lookupDef = new MetrixDropDownDef(lookupTable,
				lookupDisplayColumn, lookupValueColumn, lookupFilterColumn,
				lookupFilter);
		this.friendlyName = friendlyName;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param lookupTable
	 *            The name of the table containing data the spinner should be
	 *            populated with.
	 * @param lookupDisplayColumn
	 *            The column that should be displayed by the spinner in it's
	 *            display/value pair.
	 * @param lookupValueColumn
	 *            The value that should be used by the spinner in it's
	 *            display/value pair.
	 * @param lookupFilterColumn
	 *            A column to filter the data being used to populate the
	 *            spinner.
	 * @param lookupFilter
	 *            Data to be used when building the filter to populate the
	 *            spinner.
	 * @param orderBy
	 * 			  An order by that should be applied to the values being used
	 * 			  to populate the spinner.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * 
	 * @since 5.6 Patch 2
	 */	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, String lookupTable, String lookupDisplayColumn,
			String lookupValueColumn, String lookupFilterColumn,
			String lookupFilter, MetrixOrderByDef orderBy, String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.dataType = dataType;
		this.required = required;
		this.lookupDef = new MetrixDropDownDef(lookupTable,
				lookupDisplayColumn, lookupValueColumn, lookupFilterColumn,
				lookupFilter, orderBy);
		this.friendlyName = friendlyName;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * @param lookupTable
	 *            The name of the table containing data the spinner should be
	 *            populated with.
	 * @param lookupDisplayColumn
	 *            The column that should be displayed by the spinner in it's
	 *            display/value pair.
	 * @param lookupValueColumn
	 *            The value that should be used by the spinner in it's
	 *            display/value pair.
	 * @param lookupFilterColumn
	 *            A column to filter the data being used to populate the
	 *            spinner.
	 * @param lookupFilter
	 *            Data to be used when building the filter to populate the
	 *            spinner.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * 
	 *            <pre>
	 * MetrixTableDef taskDef = new MetrixTableDef(&quot;task&quot;,
	 * 		MetrixTransactionTypes.UPDATE);
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__metrix_row_id,
	 * 		&quot;metrix_row_id&quot;, true, double.class, &quot;Metrix Row ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_id, &quot;task_id&quot;, true,
	 * 		double.class, true, &quot;Task ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_status, &quot;task_status&quot;,
	 * 		true, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;task_status&quot;,
	 * 		&quot;task_status&quot;, &quot;description&quot;, &quot;&quot;, &quot;&quot;, &quot;Task Status&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_type, &quot;task_type&quot;,
	 * 		false, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;TASK_TYPE&quot;, &quot;Task Type&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__priority, &quot;priority&quot;, false,
	 * 		String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;PRIORITY&quot;, &quot;Priority&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__plan_start_dttm,
	 * 		&quot;plan_start_dttm&quot;, true, Date.class, &quot;Planned Start&quot;));
	 * </pre>
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, MetrixControlCase forceCase, String validation,
			String lookupTable, String lookupDisplayColumn,
			String lookupValueColumn, String lookupFilterColumn,
			String lookupFilter, String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.dataType = dataType;
		this.required = required;
		this.forceCase = forceCase;
		this.validation = validation;
		this.lookupDef = new MetrixDropDownDef(lookupTable,
				lookupDisplayColumn, lookupValueColumn, lookupFilterColumn,
				lookupFilter);
		this.friendlyName = friendlyName;
	}

	/**
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * @param lookupTable
	 *            The name of the table containing data the spinner should be
	 *            populated with.
	 * @param lookupDisplayColumn
	 *            The column that should be displayed by the spinner in it's
	 *            display/value pair.
	 * @param lookupValueColumn
	 *            The value that should be used by the spinner in it's
	 *            display/value pair.
	 * @param lookupFilterColumn
	 *            A column to filter the data being used to populate the
	 *            spinner.
	 * @param lookupFilter
	 *            Data to be used when building the filter to populate the
	 *            spinner.
	 * @param orderBy
	 * 			  An order by to apply to the data being used to populate the
	 * 			  spinner.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * 
	 * @since 5.6 Patch 2
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, MetrixControlCase forceCase, String validation,
			String lookupTable, String lookupDisplayColumn,
			String lookupValueColumn, String lookupFilterColumn,
			String lookupFilter, MetrixOrderByDef orderBy, String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.dataType = dataType;
		this.required = required;
		this.forceCase = forceCase;
		this.validation = validation;
		this.lookupDef = new MetrixDropDownDef(lookupTable,
				lookupDisplayColumn, lookupValueColumn, lookupFilterColumn,
				lookupFilter, orderBy);
		this.friendlyName = friendlyName;
	}

	/**
	 * A convenience constructor.
	 * 
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * @param lookupTable
	 *            The name of the table containing data the spinner should be
	 *            populated with.
	 * @param lookupDisplayColumn
	 *            The column that should be displayed by the spinner in it's
	 *            display/value pair.
	 * @param lookupValueColumn
	 *            The value that should be used by the spinner in it's
	 *            display/value pair.
	 * @param lookupFilterColumn
	 *            A column to filter the data being used to populate the
	 *            spinner.
	 * @param lookupFilter
	 *            Data to be used when building the filter to populate the
	 *            spinner.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * 
	 *            <pre>
	 * MetrixTableDef taskDef = new MetrixTableDef(&quot;task&quot;,
	 * 		MetrixTransactionTypes.UPDATE);
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__metrix_row_id,
	 * 		&quot;metrix_row_id&quot;, true, double.class, &quot;Metrix Row ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_id, &quot;task_id&quot;, true,
	 * 		double.class, true, &quot;Task ID&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_status, &quot;task_status&quot;,
	 * 		true, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;task_status&quot;,
	 * 		&quot;task_status&quot;, &quot;description&quot;, &quot;&quot;, &quot;&quot;, &quot;Task Status&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__task_type, &quot;task_type&quot;,
	 * 		false, String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;TASK_TYPE&quot;, &quot;Task Type&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__priority, &quot;priority&quot;, false,
	 * 		String.class, MetrixControlCase.UPPER, &quot;&quot;, &quot;global_code_table&quot;,
	 * 		&quot;code_value&quot;, &quot;description&quot;, &quot;code_name&quot;, &quot;PRIORITY&quot;, &quot;Priority&quot;));
	 * taskDef.columns.add(new MetrixColumnDef(R.id.task__plan_start_dttm,
	 * 		&quot;plan_start_dttm&quot;, true, Date.class, &quot;Planned Start&quot;));
	 * </pre>
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, boolean primaryKey, MetrixControlCase forceCase,
			String validation, String lookupTable, String lookupDisplayColumn,
			String lookupValueColumn, String lookupFilterColumn,
			String lookupFilter, String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.dataType = dataType;
		this.required = required;
		this.primaryKey = primaryKey;
		this.forceCase = forceCase;
		this.validation = validation;
		this.lookupDef = new MetrixDropDownDef(lookupTable,
				lookupDisplayColumn, lookupValueColumn, lookupFilterColumn,
				lookupFilter);
		this.friendlyName = friendlyName;
	}

	/**
	 * @param id
	 *            The R.id of the view this column should be bound to.
	 * @param columnName
	 *            The name of the database column.
	 * @param required
	 *            TRUE if the column requires a value, FALSE otherwise.
	 * @param dataType
	 *            The datatype of the column's data.
	 * @param primaryKey
	 *            TRUE if the column is part of the primary key, FALSE
	 *            otherwise.
	 * @param forceCase
	 *            MetrixControlCase defining if the value should be upper or
	 *            lower cased.
	 * @param validation
	 *            Regular expression that should be used to validate the value.
	 * @param lookupTable
	 *            The name of the table containing data the spinner should be
	 *            populated with.
	 * @param lookupDisplayColumn
	 *            The column that should be displayed by the spinner in it's
	 *            display/value pair.
	 * @param lookupValueColumn
	 *            The value that should be used by the spinner in it's
	 *            display/value pair.
	 * @param lookupFilterColumn
	 *            A column to filter the data being used to populate the
	 *            spinner.
	 * @param lookupFilter
	 *            Data to be used when building the filter to populate the
	 *            spinner.
	 * @param orderBy
	 * 			  An order by to apply to the data being used to populate the
	 * 			  spinner.
	 * @param friendlyName
	 *            The name to use for error or informational messages describing
	 *            this column.
	 * 
	 * @since 5.6 Patch 2
	 */
	public MetrixColumnDef(Integer id, String columnName, boolean required,
			Type dataType, boolean primaryKey, MetrixControlCase forceCase,
			String validation, String lookupTable, String lookupDisplayColumn,
			String lookupValueColumn, String lookupFilterColumn,
			String lookupFilter, MetrixOrderByDef orderBy, String friendlyName) {
		this.id = id;
		this.columnName = columnName;
		this.dataType = dataType;
		this.required = required;
		this.primaryKey = primaryKey;
		this.forceCase = forceCase;
		this.validation = validation;
		this.lookupDef = new MetrixDropDownDef(lookupTable,
				lookupDisplayColumn, lookupValueColumn, lookupFilterColumn,
				lookupFilter, orderBy);
		this.friendlyName = friendlyName;
	}

	@Override
	public String toString() {
		StringBuilder value = new StringBuilder();

		value.append(AndroidResourceHelper.getMessage("ColumnName1Args", this.columnName));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("DataType1Args",this.dataType));
		value.append(", ");
		value.append(AndroidResourceHelper.getMessage("Required1Args",this.required));

		return value.toString();
	}
}