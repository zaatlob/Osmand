/**
 * 
 */
package com.osmand.activities.search;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.osmand.OsmandSettings;
import com.osmand.PoiFilter;
import com.osmand.PoiFiltersHelper;
import com.osmand.R;
import com.osmand.activities.MapActivity;
import com.osmand.data.Amenity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

/**
 * @author Maxim Frolov
 * 
 */
public class SearchPOIActivity extends ListActivity {

	public static final String AMENITY_FILTER = "amenity_filter";

	private List<Amenity> amenityList;

	private Button searchPOILevel;

	private PoiFilter filter;

	private AmenityAdapter amenityAdapter;

	private LatLon lastKnownMapLocation;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.searchpoi);
		searchPOILevel = (Button) findViewById(R.id.SearchPOILevelButton);
		searchPOILevel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				amenityList = filter.searchFurther(lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude());
				amenityAdapter.setNewModel(amenityList);
				searchPOILevel.setEnabled(filter.isSearchFurtherAvailable());

			}
		});

		Bundle bundle = this.getIntent().getExtras();
		String filterId = bundle.getString(AMENITY_FILTER);
		if (filterId != null) {
			lastKnownMapLocation = OsmandSettings.getLastKnownMapLocation(this);
			filter = PoiFiltersHelper.getFilterById(this, filterId);
			amenityAdapter = new AmenityAdapter(filter.initializeNewSearch(lastKnownMapLocation.getLatitude(), 
					lastKnownMapLocation.getLongitude(), 40));
			setListAdapter(amenityAdapter);
		}
		// ListActivity has a ListView, which you can get with:
		ListView lv = getListView();

		// Then you can create a listener like so:
		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
				Amenity amenity = amenityList.get(pos);
				String format = amenity.getSimpleFormat(OsmandSettings.usingEnglishNames(v.getContext()));
				if (amenity.getOpeningHours() != null) {
					format += "\nOpening hours : " + amenity.getOpeningHours();
				}
				Toast.makeText(v.getContext(), format, Toast.LENGTH_LONG).show();
				return true;
			}
		});
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, MODE_WORLD_READABLE);
		if (prefs != null) {
			Amenity amenity = amenityList.get(position);
			OsmandSettings.setMapLocationToShow(this, amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude());
			Intent newIntent = new Intent(SearchPOIActivity.this, MapActivity.class);
			startActivity(newIntent);
		}
	}

	class AmenityAdapter extends ArrayAdapter<Amenity> {
		AmenityAdapter(List<Amenity> list) {
			super(SearchPOIActivity.this, R.layout.searchpoi_list, list);
			this.setNotifyOnChange(false);
		}

		public void setNewModel(List<Amenity> amenityList) {
			setNotifyOnChange(false);
			((AmenityAdapter) getListAdapter()).clear();
			for (Amenity obj : amenityList) {
				this.add(obj);
			}
			this.notifyDataSetChanged();

		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.searchpoi_list, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.poi_label);
			TextView distanceLabel = (TextView) row.findViewById(R.id.poidistance_label);
			ImageView icon = (ImageView) row.findViewById(R.id.poi_icon);
			Amenity amenity = getItem(position);
			LatLon lastKnownMapLocation = OsmandSettings.getLastKnownMapLocation(SearchPOIActivity.this);
			int dist = (int) (MapUtils.getDistance(amenity.getLocation(), lastKnownMapLocation.getLatitude(), lastKnownMapLocation
					.getLongitude()));
			String str = amenity.getStringWithoutType(OsmandSettings.usingEnglishNames(SearchPOIActivity.this));
			label.setText(str);
			if (amenity.getOpeningHours() != null) {
				icon.setImageResource(R.drawable.poi);
			} else {
				icon.setImageResource(R.drawable.closed_poi);
			}
			
			distanceLabel.setText(" " + MapUtils.getFormattedDistance(dist));
			return (row);
		}
	}

}
