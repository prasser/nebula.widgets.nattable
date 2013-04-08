/*******************************************************************************
 * Copyright (c) 2013 Dirk Fauth and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dirk Fauth <dirk.fauth@gmail.com> - initial API and implementation
 *******************************************************************************/ 
package org.eclipse.nebula.widgets.nattable.extension.glazedlists.filterrow;

import static org.eclipse.nebula.widgets.nattable.filterrow.FilterRowDataLayer.FILTER_ROW_COLUMN_LABEL_PREFIX;
import static org.eclipse.nebula.widgets.nattable.filterrow.config.FilterRowConfigAttributes.FILTER_DISPLAY_CONVERTER;
import static org.eclipse.nebula.widgets.nattable.style.DisplayMode.NORMAL;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;
import org.eclipse.nebula.widgets.nattable.filterrow.combobox.FilterRowComboBoxDataProvider;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.CompositeMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor;

/**
 * Specialisation of the DefaultGlazedListsStaticFilterStrategy that is intended to be used in 
 * combination with FilterRowComboBoxCellEditors that allows filtering via multiselect
 * comboboxes like in Excel. As it extends DefaultGlazedListsStaticFilterStrategy it also supports
 * static filters which allows to integrate it with the GlazedListsRowHideShowLayer.
 * <p>
 * The special case in here is that if nothing is selected in the filter combobox, then 
 * everything should be filtered.
 * 
 * @author Dirk Fauth
 *
 */
public class ComboBoxGlazedListsFilterStrategy<T> extends DefaultGlazedListsStaticFilterStrategy<T> {

	/**
	 * The FilterRowComboBoxDataProvider needed to determine whether filters should applied or not. 
	 * If there are no values specified for filtering of a column then everything should be filtered, 
	 * if all possible values are given as filter then no filter needs to be applied.
	 */
	private FilterRowComboBoxDataProvider<T> comboBoxDataProvider;
	
	/**
	 * A MatcherEditor that will never match anything.
	 */
	private MatcherEditor<T> matchNone = new AbstractMatcherEditor<T>() {
		{
			fireMatchNone();
		}
	};
	
	/**
	 * 
	 * @param comboBoxDataProvider The FilterRowComboBoxDataProvider needed to determine whether
	 * 			filters should applied or not. If there are no values specified for filtering of
	 * 			a column then everything should be filtered, if all possible values are given as
	 * 			filter then no filter needs to be applied.
	 * @param matcherEditor The CompositeMatcherEditor that is used for GlazedLists filtering
	 * @param columnAccessor The IColumnAccessor needed to access the row data to perform filtering
	 * @param configRegistry The IConfigRegistry to retrieve several configurations from
	 */
	public ComboBoxGlazedListsFilterStrategy(FilterRowComboBoxDataProvider<T> comboBoxDataProvider, 
			CompositeMatcherEditor<T> matcherEditor,
			IColumnAccessor<T> columnAccessor, IConfigRegistry configRegistry) {
		super(matcherEditor, columnAccessor, configRegistry);
		this.comboBoxDataProvider = comboBoxDataProvider;
	}

	
	@SuppressWarnings("rawtypes")
	@Override
	public void applyFilter(Map<Integer, Object> filterIndexToObjectMap) {
		if (filterIndexToObjectMap.isEmpty()) {
			this.matcherEditor.getMatcherEditors().add(matchNone);
			return;
		}
		
		//we need to create a new Map for applying a filter using the parent class
		//otherwise we would remove the previous added pre-selected values
		Map<Integer, Object> newIndexToObjectMap = new HashMap<Integer, Object>();
		newIndexToObjectMap.putAll(filterIndexToObjectMap);
		
		for (Integer index : this.comboBoxDataProvider.getCachedColumnIndexes()) {
			List<?> dataProviderList = this.comboBoxDataProvider.getValues(index, 0);
			Object filterObject = newIndexToObjectMap.get(index);
			if (filterObject == null || (filterObject instanceof Collection && ((Collection)filterObject).isEmpty())) {
				//for one column there are no items selected in the combo, therefore nothing matches
				this.matcherEditor.getMatcherEditors().add(matchNone);
				return;
			} else if (filterObject instanceof Collection 
					&& ((Collection)filterObject).size() == dataProviderList.size()) {
				newIndexToObjectMap.remove(index);
			}
		}
		
		super.applyFilter(newIndexToObjectMap);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation is able to handle Collections and will generate a regular expression containing
	 * all values in the Collection.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected String getStringFromColumnObject(final int columnIndex, final Object object) {
		final IDisplayConverter displayConverter = 
				this.configRegistry.getConfigAttribute(
						FILTER_DISPLAY_CONVERTER, NORMAL, FILTER_ROW_COLUMN_LABEL_PREFIX + columnIndex);
		
		if (object instanceof Collection) {
			String result = ""; //$NON-NLS-1$
			Collection valueCollection = (Collection)object;
			for (Object value : valueCollection) {
				if (result.length() > 0) {
					result += "|"; //$NON-NLS-1$
				}
				String convertedValue = displayConverter.canonicalToDisplayValue(value).toString(); 
				if (convertedValue.isEmpty()) {
					//for an empty String add the regular expression for empty String
					convertedValue = "^$"; //$NON-NLS-1$
				}
				result += convertedValue;
			}
			return "(" + result + ")";  //$NON-NLS-1$//$NON-NLS-2$
		}

		return displayConverter.canonicalToDisplayValue(object).toString();
	}

}
