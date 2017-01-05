/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.api.editor.quickfix;

import org.eclipse.che.ide.api.editor.texteditor.TextEditor;

import javax.validation.constraints.NotNull;

/** Factory of {@link QuickAssistAssistant}. */
public interface QuickAssistantFactory {

    /**
     * Create a QuickAssist assistant instance.
     * @param textEditor the related editor
     * @return a new quick assist assistant
     */
    @NotNull
    QuickAssistAssistant createQuickAssistant(TextEditor textEditor);
}
