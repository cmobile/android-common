/*
 * Copyright 2013 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact details
 *
 * Email: se.solovyev@gmail.com
 * Site:  http://se.solovyev.org
 */

package org.solovyev.android.samples.db;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import org.solovyev.android.App;
import org.solovyev.android.async.CommonAsyncTask;
import org.solovyev.android.list.ListItem;
import org.solovyev.android.list.ListItemAdapter;
import org.solovyev.android.samples.Locator;
import org.solovyev.android.samples.R;
import org.solovyev.common.text.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: serso
 * Date: 8/10/12
 * Time: 4:28 PM
 */
public class SamplesDbActivity extends ListActivity {

	/*
	**********************************************************************
	*
	*                           VIEWS
	*
	**********************************************************************
	*/
	@Nonnull
	private Button addItemButton;

	@Nonnull
	private EditText addButtonName;

	@Nonnull
	private Button removeItemButton;

	@Nonnull
	private EditText removeItemName;

	@Nonnull
	private EditText itemFilter;

    /*
	**********************************************************************
    *
    *                           METHODS
    *
    **********************************************************************
    */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.acl_db_layout);

		addItemButton = (Button) findViewById(R.id.add_item_button);
		addButtonName = (EditText) findViewById(R.id.add_item_name);
		removeItemButton = (Button) findViewById(R.id.remove_item_button);
		removeItemName = (EditText) findViewById(R.id.remove_item_name);
		itemFilter = (EditText) findViewById(R.id.item_filter);

		addItemButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String itemName = addButtonName.getText().toString();
				if (Strings.isEmpty(itemName)) {
					Toast.makeText(SamplesDbActivity.this, getString(R.string.name_is_empty), Toast.LENGTH_SHORT).show();
				} else {
					addItem(itemName);
				}
			}
		});

		removeItemButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String itemName = removeItemName.getText().toString();
				if (Strings.isEmpty(itemName)) {
					Toast.makeText(SamplesDbActivity.this, getString(R.string.name_is_empty), Toast.LENGTH_SHORT).show();
				} else {
					removeItem(itemName);
				}
			}
		});

		itemFilter.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				final String filter = s.toString();

				final ListItemAdapter adapter = getListItemAdapter();
				adapter.clear();
				loadDbItems(filter);
			}
		});

		ListItemAdapter.createAndAttach(this, new ArrayList<ListItem>());

		loadDbItems(null);
	}

	private void loadDbItems(@Nullable final String filter) {
		new CommonAsyncTask<Void, Void, List<DbItemListItem>>(this) {

			@Override
			protected List<DbItemListItem> doWork(@Nonnull List<Void> voids) {

				final Context context = getContext();

				final List<DbItemListItem> result;
				if (context != null) {
					result = new ArrayList<DbItemListItem>();

					final List<DbItem> dbItems;
					if (Strings.isEmpty(filter)) {
						dbItems = getDbItemService().getAllDbItems(context);
					} else {
						// NOTE: filter can be applied to Adapter. Usage here only in academical purposes
						assert filter != null;
						dbItems = getDbItemService().getAllStartsWith(filter, context);
					}

					for (DbItem dbItem : dbItems) {
						result.add(new DbItemListItem(dbItem));
					}
				} else {
					result = Collections.emptyList();
				}

				return result;
			}

			@Override
			protected void onSuccessPostExecute(@Nullable List<DbItemListItem> result) {
				getListItemAdapter().addAll(result);
			}

			@Override
			protected void onFailurePostExecute(@Nonnull Exception e) {
				Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		}.execute((Void) null);
	}

	@SuppressWarnings("unchecked")
	@Nonnull
	private ListItemAdapter<DbItemListItem> getListItemAdapter() {
		return (ListItemAdapter<DbItemListItem>) getListView().getAdapter();
	}

	private void removeItem(@Nonnull String itemName) {
		new CommonAsyncTask<String, Void, List<DbItem>>(this) {

			@Override
			protected List<DbItem> doWork(@Nonnull List<String> params) {
				assert params.size() == 1;
				final String itemName = params.get(0);

				final Context context = getContext();
				if (context != null) {
					return getDbItemService().removeItemByName(itemName, context);
				}

				return Collections.emptyList();
			}

			@Override
			protected void onSuccessPostExecute(@Nullable List<DbItem> result) {
				assert result != null;

				SamplesDbActivity.this.removeItemName.setText("");
				Toast.makeText(getContext(), getString(R.string.items_removed, result.size()), Toast.LENGTH_SHORT).show();
				final ListItemAdapter<DbItemListItem> adapter = getListItemAdapter();
				for (DbItem dbItem : result) {
					adapter.remove(new DbItemListItem(dbItem));
				}
			}

			@Override
			protected void onFailurePostExecute(@Nonnull Exception e) {
				Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		}.execute(itemName);
	}

	private void addItem(@Nonnull String itemName) {
		new CommonAsyncTask<String, Void, DbItem>(this) {

			@Override
			protected DbItem doWork(@Nonnull List<String> params) {
				assert params.size() == 1;
				final String itemName = params.get(0);
				final DbItemImpl result = new DbItemImpl(itemName);

				final Context context = getContext();
				if (context != null) {
					getDbItemService().addItem(result, context);
				}

				return result;
			}

			@Override
			protected void onSuccessPostExecute(@Nullable DbItem result) {
				assert result != null;

				SamplesDbActivity.this.addButtonName.setText("");
				Toast.makeText(getContext(), getString(R.string.item_saved), Toast.LENGTH_SHORT).show();
				getListItemAdapter().add(new DbItemListItem(result));
			}

			@Override
			protected void onFailurePostExecute(@Nonnull Exception e) {
				Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		}.execute(itemName);
	}

	@Nonnull
	private DbItemService getDbItemService() {
		return ((Locator) App.getLocator()).getDbItemService();
	}
}
