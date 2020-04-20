/*
 * Copyright (c) 1998-2020 by Richard A. Wilkes. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, version 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as
 * defined by the Mozilla Public License, version 2.0.
 */

package com.trollworks.gcs.menu.item;

import com.trollworks.gcs.common.HasSourceReference;
import com.trollworks.gcs.library.LibraryExplorerDockable;
import com.trollworks.gcs.pdfview.PdfDockable;
import com.trollworks.gcs.pdfview.PdfRef;
import com.trollworks.gcs.ui.widget.outline.ListOutline;
import com.trollworks.gcs.collections.ReverseListIterator;
import com.trollworks.gcs.ui.Selection;
import com.trollworks.gcs.menu.Command;
import com.trollworks.gcs.ui.widget.StdFileDialog;
import com.trollworks.gcs.ui.widget.outline.OutlineModel;
import com.trollworks.gcs.ui.widget.outline.OutlineProxy;
import com.trollworks.gcs.ui.widget.outline.Row;
import com.trollworks.gcs.utility.FileType;
import com.trollworks.gcs.utility.I18n;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Provides the "Open Page Reference" command. */
public class OpenPageReferenceCommand extends Command {
    /** The singleton {@link OpenPageReferenceCommand} for opening a single page reference. */
    public static final OpenPageReferenceCommand OPEN_ONE_INSTANCE  = new OpenPageReferenceCommand(true, KeyEvent.VK_G, COMMAND_MODIFIER);
    /** The singleton {@link OpenPageReferenceCommand} for opening all page references. */
    public static final OpenPageReferenceCommand OPEN_EACH_INSTANCE = new OpenPageReferenceCommand(false, KeyEvent.VK_G, SHIFTED_COMMAND_MODIFIER);
    private             ListOutline              mOutline;

    private OpenPageReferenceCommand(boolean one, int key, int modifiers) {
        super(getTitle(one), getCmd(one), key, modifiers);
    }

    /**
     * Creates a new {@link OpenPageReferenceCommand}.
     *
     * @param outline The outline to work against.
     * @param one     Whether to open just the first page reference, or all of them.
     */
    public OpenPageReferenceCommand(ListOutline outline, boolean one) {
        super(getTitle(one), getCmd(one));
        mOutline = outline;
    }

    private static String getTitle(boolean one) {
        return one ? I18n.Text("Open Page Reference") : I18n.Text("Open Each Page Reference");
    }

    private static String getCmd(boolean one) {
        return one ? "OpenPageReference" : "OpenEachPageReferences";
    }

    @Override
    public void adjust() {
        setEnabled(!getReferences(getTarget()).isEmpty());
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        HasSourceReference target = getTarget();
        if (target != null) {
            List<String> references = getReferences(target);
            if (!references.isEmpty()) {
                String highlight = target.getReferenceHighlight();
                if (this == OPEN_ONE_INSTANCE) {
                    openReference(references.get(0), highlight);
                } else {
                    for (String one : new ReverseListIterator<>(references)) {
                        openReference(one, highlight);
                    }
                }
            }
        }
    }

    public static void openReference(String reference, String highlight) {
        int i = reference.length() - 1;
        while (i >= 0) {
            char ch = reference.charAt(i);
            if (ch >= '0' && ch <= '9') {
                i--;
            } else {
                i++;
                break;
            }
        }
        if (i > 0) {
            String id = reference.substring(0, i);
            try {
                int    page = Integer.parseInt(reference.substring(i));
                PdfRef ref  = PdfRef.lookup(id, true);
                if (ref == null) {
                    File file = StdFileDialog.showOpenDialog(getFocusOwner(), String.format(I18n.Text("Locate the PDF file for the prefix \"%s\""), id), FileType.PDF.getFilter());
                    if (file != null) {
                        ref = new PdfRef(id, file, 0);
                        ref.save();
                    }
                }
                if (ref != null) {
                    Path                    path    = ref.getFile().toPath();
                    LibraryExplorerDockable library = LibraryExplorerDockable.get();
                    if (library != null) {
                        PdfDockable dockable = (PdfDockable) library.getDockableFor(path);
                        if (dockable != null) {
                            dockable.goToPage(ref, page, highlight);
                            dockable.getDockContainer().setCurrentDockable(dockable);
                        } else {
                            dockable = new PdfDockable(ref, page, highlight);
                            library.dockPdf(dockable);
                            library.open(path);
                        }
                    }
                }
            } catch (NumberFormatException nfex) {
                // Ignore
            }
        }
    }

    private HasSourceReference getTarget() {
        HasSourceReference ref     = null;
        ListOutline        outline = mOutline;
        if (outline == null) {
            Component comp = getFocusOwner();
            if (comp instanceof OutlineProxy) {
                comp = ((OutlineProxy) comp).getRealOutline();
            }
            if (comp instanceof ListOutline) {
                outline = (ListOutline) comp;
            }
        }
        if (outline != null) {
            OutlineModel model = outline.getModel();
            if (model.hasSelection()) {
                Selection selection = model.getSelection();
                if (selection.getCount() == 1) {
                    Row row = model.getFirstSelectedRow();
                    if (row instanceof HasSourceReference) {
                        ref = (HasSourceReference) row;
                    }
                }
            }
        }
        return ref;
    }

    private static List<String> getReferences(HasSourceReference ref) {
        List<String> list = new ArrayList<>();
        if (ref != null) {
            String[] refs = ref.getReference().split("[,;]");
            if (refs.length > 0) {
                for (String one : refs) {
                    String trimmed = one.trim();
                    if (!trimmed.isEmpty()) {
                        list.add(trimmed);
                    }
                }
            }
        }
        return list;
    }
}