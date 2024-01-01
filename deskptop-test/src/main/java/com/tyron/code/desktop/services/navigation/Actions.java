package com.tyron.code.desktop.services.navigation;

import com.tyron.code.desktop.ui.docking.DockingManager;
import com.tyron.code.desktop.ui.docking.DockingRegion;
import com.tyron.code.desktop.ui.docking.DockingTab;
import com.tyron.code.desktop.ui.pane.editing.SourcePane;
import com.tyron.code.desktop.util.Icons;
import com.tyron.code.path.PathNode;
import com.tyron.code.path.impl.SourceClassPathNode;
import com.tyron.code.project.model.module.Module;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class Actions {

    @NotNull
    private final NavigationManager navigationManager;
    @NotNull
    private final DockingManager dockingManager;

    public Actions(@NotNull NavigationManager navigationManager, @NotNull DockingManager dockingManager) {
        this.navigationManager = navigationManager;
        this.dockingManager = dockingManager;
    }

    @NotNull
    public SourceFileNavigable gotoDeclaration(@NotNull SourceClassPathNode path) {
        Module module = path.getValueOfType(Module.class);

        return (SourceFileNavigable) getOrCreatePathContent(path, () -> {
            String title = "Test.java";
            Node graphic = Icons.getIconView(Icons.CLASS);

            SourcePane content = new SourcePane(module);
            content.onUpdatePath(path);

            DockingTab tab = createTab(dockingManager.getPrimaryRegion(), title, graphic, content);
//            content.addPathUpdateListener(updatedPath -> {
//                // Update tab graphic in case backing class details change.
//                AndroidClassInfo updatedInfo = updatedPath.getValue().asAndroidClass();
//                String updatedTitle = textService.getAndroidClassInfoTextProvider(workspace, resource, bundle, updatedInfo).makeText();
//                Node updatedGraphic = iconService.getAndroidClassInfoIconProvider(workspace, resource, bundle, updatedInfo).makeIcon();
//                FxThreadUtil.run(() -> {
//                    tab.setText(updatedTitle);
//                    tab.setGraphic(updatedGraphic);
//                });

            return tab;

        });
    }

    /**
     * Shorthand for tab-creation + graphic setting.
     *
     * @param region
     * 		Parent region to spawn in.
     * @param title
     * 		Tab title.
     * @param graphic
     * 		Tab graphic.
     * @param content
     * 		Tab content.
     *
     * @return Created tab.
     */
    private static DockingTab createTab(@NotNull DockingRegion region,
                                        @NotNull String title,
                                        @NotNull Node graphic,
                                        @NotNull Node content) {
        DockingTab tab = region.createTab(title, content);
        tab.setGraphic(graphic);
        return tab;
    }

    /**
     * Looks for the {@link Navigable} component representing the path and returns it if found.
     * If no such component exists, it should be generated by the passed supplier, which then gets returned.
     * <br>
     * The tab containing the {@link Navigable} component is selected when returned.
     *
     * @param path
     * 		Path to navigate to.
     * @param factory
     * 		Factory to create a tab for displaying content located at the given path,
     * 		should a tab for the content not already exist.
     * 		<br>
     * 		<b>NOTE:</b> It is required/assumed that the {@link Tab#getContent()} is a
     * 		component implementing {@link Navigable}.
     *
     * @return Navigable content representing content of the path.
     */
    @NotNull
    public Navigable getOrCreatePathContent(@NotNull PathNode<?> path, @NotNull Supplier<DockingTab> factory) {
        List<Navigable> children = navigationManager.getNavigableChildrenByPath(path);
        if (children.isEmpty()) {
            // Create the tab for the content, then display it.
            DockingTab tab = factory.get();
            tab.select();
            return (Navigable) tab.getContent();
        } else {
            // Content by path is already open.
            Navigable navigable = children.get(0);
            selectTab(navigable);
            navigable.requestFocus();
            return navigable;
        }
    }

    /**
     * Selects the containing {@link DockingTab} that contains the content.
     *
     * @param navigable
     * 		Navigable content to select in its containing {@link DockingRegion}.
     */
    private static void selectTab(Navigable navigable) {
        if (navigable instanceof Node node) {
            while (node != null) {
                // Get the parent of the node, skip the intermediate 'content area' from tab-pane default skin.
                Parent parent = node.getParent();
                if (parent.getStyleClass().contains("tab-content-area"))
                    parent = parent.getParent();

                // If the tab content is the node, select it and return.
                if (parent instanceof DockingRegion tabParent)
                    for (DockingTab tab : tabParent.getDockTabs())
                        if (tab.getContent() == node) {
                            tab.select();
                            return;
                        }

                // Next parent.
                node = parent;
            }
        }
    }
}
