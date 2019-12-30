package com.example.tryhopper;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

//import org.oscim.android.MapView;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {
    //private static final String MAP_FILE = "canary_islands.map";
    private static final String MAP_FILE = "Ireland_ML.map";
    //private TileOverlay tileOverlay;
    private MapView mapView;
    private TileCache tileCache;
    //private TileRendererLayer tileRendererLayer;
    private ArrayList<View> visiblePopups = new ArrayList<>();

    private String markerObject = "marker";

    private GraphHopper hopper;

    //private LatLng firstPoint = new LatLng(28.5696778, -16.1596187), secondPoint = new LatLng(28.5492227, -16.1933043), centerPoint = new LatLng(28.4698336, -16.3259077);
    private LatLng firstPoint = new LatLng(53.3438, -6.2546), secondPoint = new LatLng(53.3082, -6.2241), centerPoint = new LatLng(53.33306, -6.24889);

    private String grassHopperFolder;
    private String grassHopperAlerterFolder;
    private TileRendererLayer tileRendererLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
*/

        grassHopperFolder = getFilesDir() + "/grasshopper";
        grassHopperAlerterFolder = getFilesDir() + "/grassHopperAlerterFolder";
        if (!(new File(grassHopperFolder).exists())) {
            new File(grassHopperFolder).mkdir();
        }
        if (!(new File(grassHopperAlerterFolder).exists())) {
            new File(grassHopperAlerterFolder).mkdir();
        }

        new AsyncService().execute();

    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
    private void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        if (files != null)
            for (String filename : files) {
                //if(!filename.equals("grasshoper.zip"))
                if(!filename.equals("ireland.zip"))
                    continue;
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = assetManager.open(filename);
                    File outFile = new File(grassHopperFolder, filename);
                    if (outFile.exists()) {
                        return;
                    }
                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                } catch (IOException e) {
                    Log.e("tag", "Failed to copy asset file: " + filename, e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                }
            }
    }
    private boolean unpackZip(String path, String zipname) {
        InputStream is;
        ZipInputStream zis;
        try {
            String filename;
            is = new FileInputStream(path +"/"+ zipname);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                // zapis do souboru
                filename = ze.getName();

                // Need to create directories if not exists, or
                // it will generate an Exception...

                if (filename.contains("__MACOSX")){
                    continue;
                }
                if (ze.isDirectory()) {
                    File fmd = new File(path +"/"+ filename);
                    fmd.mkdirs();
                    continue;
                }

                File outFile = new File(path +"/"+ filename);
                if (outFile.exists()) {
                    continue;
                }
                FileOutputStream fout = new FileOutputStream(path +"/"+ filename);


                // cteni zipu a zapis
                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }

                fout.close();
                zis.closeEntry();
            }

            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void getDirections() {
        // create one GraphHopper instance
        hopper = new GraphHopper().forMobile();
        //		hopper.setDataReaderFile(Environment.getExternalStorageDirectory()+"/Downloads/tenerife.osm");
        //		grassHopperOsmFile = new File(Environment.getExternalStorageDirectory()+"/Downloads/tenerife.osm").getAbsolutePath();
        //		hopper.setDataReaderFile(grassHopperOsmFile);

        // where to store graphhopper files?


        hopper.setGraphHopperLocation(grassHopperFolder);

        // now this can take minutes if it imports or a few seconds for loading
        // of course this is dependent on the area you import
        hopper.load(grassHopperFolder);

        GHRequest req = new GHRequest(firstPoint.latitude, firstPoint.longitude, secondPoint.latitude, secondPoint.longitude).setAlgorithm(Parameters.Algorithms.DIJKSTRA_BI);//setWeighting("fastest");
        GHResponse rsp = hopper.route(req);

        // first check for errors
        if (rsp.hasErrors()) {
            // handle them!
            // rsp.getErrors()
            return;
        }

        // use the best path, see the GHResponse class for more possibilities.
		/*PathWrapper path = rsp.getBest();

		// points, distance in meters and time in millis of the full path
		PointList pointList = path.getPoints();
		double distance = path.getDistance();
		long timeInMs = path.getTime();

		InstructionList il = path.getInstructions();
		// iterate over every turn instruction
		for(Instruction instruction : il) {
			instruction.getDistance();
		}

		// or get the json
		List<Map<String, Object>> iList = il.createJson();

		// or get the result as gpx entries:
		List<GPXEntry> list = il.createGPXList();*/

        Polyline polyline = createPolyline(rsp);

        this.mapView.getLayerManager().getLayers().add(polyline);
        hopper.close();
        //		findAlternateDiraction();
    }
    private Polyline createPolyline(GHResponse response) {
        GraphicFactory gf = AndroidGraphicFactory.INSTANCE;
        Paint paint = gf.createPaint();
        paint.setColor(AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK));
        paint.setStyle(Style.STROKE);
        paint.setDashPathEffect(new float[]{25, 15});
        paint.setStrokeWidth(8);
        Polyline line = new Polyline(paint, AndroidGraphicFactory.INSTANCE);

        List<LatLong> geoPoints = line.getLatLongs();
        PointList tmp = response.getBest().getPoints();
        for (int i = 0; i < response.getBest().getPoints().getSize(); i++) {
            geoPoints.add(new LatLong(tmp.getLatitude(i), tmp.getLongitude(i)));
        }

        return line;
    }

    private void extractZips() {
        copyAssets();
        //unpackZip(grassHopperFolder, "grasshoper.zip");;
        unpackZip(grassHopperFolder, "ireland.zip");
    }
    class AsyncService  extends AsyncTask<Void,Void,Void> {

        @Override protected Void doInBackground(Void... voids) {
            File outFile = new File(grassHopperFolder, "ireland.zip");
            if (outFile.exists()) {
                return null;
            }
            extractZips();
            return null;
        }

        @Override protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            setUpMap();


            getDirections();
        }
    }
    private void setUpMap() {
        AndroidGraphicFactory.createInstance(this.getApplication());
        this.mapView = new MapView(this);
        setContentView(this.mapView);
        this.mapView.setClickable(true);
        this.mapView.getMapScaleBar().setVisible(true);
        this.mapView.setBuiltInZoomControls(true);
        this.tileCache = AndroidUtil.createTileCache(this, "mapcache", mapView.getModel().displayModel.getTileSize(), 1f, this.mapView.getModel().frameBufferModel.getOverdrawFactor());

        MapDataStore mapDataStore = new MapFile(new File(grassHopperFolder, MAP_FILE));
        this.tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore, this.mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);
        tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.DEFAULT);
        this.mapView.getLayerManager().getLayers().add(tileRendererLayer);
        this.mapView.setCenter(new LatLong(firstPoint.latitude, firstPoint.longitude));
        createPositionMarker(firstPoint.latitude, firstPoint.longitude);
        this.mapView.setZoomLevel((byte) 17);
    }
    private void createPositionMarker(double paramDouble1, double paramDouble2) {
        addMarkerPopup(paramDouble1, paramDouble2);
    }
    private void addMarkerPopup(double paramDouble1, double paramDouble2) {
        LatLong latLong = new LatLong(paramDouble1, paramDouble2);
        final View popUp = getLayoutInflater().inflate(R.layout.map_popup, mapView, false);
        popUp.findViewById(R.id.ivMarker).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                View popupview = popUp.findViewById(R.id.llMarkerData);
                visiblePopups.add(popupview);
                popupview.setVisibility(View.VISIBLE);
            }
        });
        mapView.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.d("mapview", "onTouch");
                for (int index = 0; index < visiblePopups.size(); index++) {
                    visiblePopups.get(index).setVisibility(View.GONE);
                    visiblePopups.remove(index--);
                }
                return false;
            }
        });
        MapView.LayoutParams mapParams = new MapView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                latLong,
                MapView.LayoutParams.Alignment.BOTTOM_CENTER);

        mapView.addView(popUp, mapParams);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
