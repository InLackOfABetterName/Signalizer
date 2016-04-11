package org.cubyte.trafficsignalizer.ui;

import com.google.inject.Inject;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawableReceiver;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.InfoText;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static java.lang.Math.round;
import static org.matsim.core.utils.misc.ByteBufferUtils.getString;
import static org.matsim.core.utils.misc.ByteBufferUtils.putString;

public class TextObject {
    public final String id;
    public final String text;
    public final float x;
    public final float y;
    public final boolean screenSpace;
    private InfoText infoText = null;

    public TextObject(String id, String text, float x, float y, boolean screenSpace) {
        this.id = id;
        this.text = text;
        this.x = x;
        this.y = y;
        this.screenSpace = screenSpace;
    }

    public synchronized InfoText getInfoText() {
        if (this.infoText == null) {
            this.infoText = new InfoText(this.text, this.x, this.y);
        }
        return this.infoText;
    }

    /**
     * Serialize on server (simulation) side
     */
    public static class Writer extends OTFDataWriter<Void> {

        private Queue<TextObject> textObjects;

        @Inject
        public Writer() {
            this.textObjects = new ArrayDeque<>();
        }

        @Override
        public void writeConstData(ByteBuffer out) throws IOException {
        }

        @Override
        public void writeDynData(ByteBuffer out) throws IOException {
            out.putInt(textObjects.size());
            for (TextObject object : textObjects) {
                putString(out, object.id);
                putString(out, object.text);
                out.putFloat(object.x);
                out.putFloat(object.y);
                out.put((byte) (object.screenSpace ? 1 : 0));
            }
            textObjects.clear();
        }

        public void put(TextObject obj) {
            this.textObjects.offer(obj);
        }

        public void put(String id, String text, float x, float y, boolean screenSpace) {
            this.put(new TextObject(id, text, x, y, screenSpace));
        }

        public void put(String id, String text, double x, double y, boolean screenSpace) {
            this.put(id, text, (float)x, (float) y, screenSpace);
        }
    }

    /**
     * Deserialize on client (visualization) side
     */
    public static class Reader extends OTFDataReader {

        private final Drawer drawer = new Drawer();

        @Override
        public void readConstData(ByteBuffer in) throws IOException {
        }

        @Override
        public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
            final int count = in.getInt();
            for (int i = 0; i < count; ++i) {
                final String id = getString(in);
                final String s = getString(in);
                final float x = in.getFloat();
                final float y = in.getFloat();
                final boolean screenSpace = in.get() != 0;
                final TextObject textObject = new TextObject(id, s, x, y, screenSpace);

                this.drawer.put(textObject);
            }
        }

        @Override
        public void invalidate(SceneGraph graph) {
            drawer.addToSceneGraph(graph);
        }
    }

    public static class Drawer extends OTFGLAbstractDrawableReceiver {

        TextRenderer textRenderer;
        private Map<String, TextObject> objects = new HashMap<>();

        public Drawer() {
            Font font = new Font("SansSerif", Font.PLAIN, 100);
            this.textRenderer = new TextRenderer(font, true, false);
        }

        @Override
        public void addToSceneGraph(SceneGraph graph) {
            graph.addItem(this);
        }

        @Override
        protected void onDraw(GL2 gl) {
            final OTFOGLDrawer drawer = OTFClientControl.getInstance().getMainOTFDrawer();
            for (TextObject object : this.objects.values()) {
                if (object.screenSpace) {
                    textRenderer.beginRendering(getDrawable().getSurfaceWidth(), getDrawable().getSurfaceHeight());
                    textRenderer.setColor(Color.black);
                    textRenderer.draw(object.text, round(object.x), round(object.y));
                    textRenderer.endRendering();
                } else {
                    object.getInfoText().draw(drawer.getTextRenderer(), getDrawable(), drawer.getViewBoundsAsQuadTreeRect());
                }
            }
        }

        public void put(TextObject textObject) {
            this.objects.put(textObject.id, textObject);
        }
    }
}
