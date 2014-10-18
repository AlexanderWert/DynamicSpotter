/**
 * Copyright 2014 SAP AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spotter.eclipse.ui.navigator;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.spotter.eclipse.ui.Activator;
import org.spotter.eclipse.ui.jobs.JobsContainer;
import org.spotter.eclipse.ui.util.DialogUtils;
import org.spotter.eclipse.ui.view.ResultsView;

/**
 * An element that represents a run result node.
 * 
 * @author Denis Knoepfle
 * 
 */
public class SpotterProjectRunResult implements IOpenableProjectElement, IDeletable {

	public static final String IMAGE_PATH = "icons/results.gif"; //$NON-NLS-1$

	private static final String ELEMENT_TYPE_NAME = "Result Item";

	private final ISpotterProjectElement parent;
	private final IFolder resultFolder;
	private Image image;
	private final long jobId;
	private final long timestamp;
	private final String elementName;

	/**
	 * Creates a new instance of this element.
	 * 
	 * @param parent
	 *            the parent element
	 * @param jobId
	 *            the corresponding job id of this run result
	 * @param timestamp
	 *            the corresponding timestamp of this run result
	 * @param resultFolder
	 *            the result folder that is represented by this node
	 */
	public SpotterProjectRunResult(ISpotterProjectElement parent, long jobId, long timestamp, IFolder resultFolder) {
		this.parent = parent;
		this.jobId = jobId;
		this.timestamp = timestamp;

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd (HH:mm:ss)");
		this.elementName = dateFormat.format(new Date(timestamp));

		this.resultFolder = resultFolder;
	}

	@Override
	public String getText() {
		return elementName;
	}

	@Override
	public Image getImage() {
		if (image == null) {
			image = Activator.getImage(IMAGE_PATH);
		}

		return image;
	}

	/**
	 * @return the corresponding timestamp of this run result
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the result folder this element is linked to
	 */
	public IFolder getResultFolder() {
		return resultFolder;
	}

	@Override
	public ISpotterProjectElement[] getChildren() {
		return SpotterProjectParent.NO_CHILDREN;
	}

	@Override
	public boolean hasChildren() {
		return false;
	}

	@Override
	public Object getParent() {
		return parent;
	}

	@Override
	public IProject getProject() {
		return parent.getProject();
	}

	@Override
	public void open() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			ResultsView view = (ResultsView) page.showView(ResultsView.VIEW_ID);
			view.setResult(this);
		} catch (PartInitException e) {
			throw new RuntimeException("Could not show view " + getOpenId(), e);
		}
	}

	@Override
	public String getOpenId() {
		return ResultsView.VIEW_ID;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SpotterProjectRunResult)) {
			return false;
		}
		SpotterProjectRunResult other = (SpotterProjectRunResult) obj;
		boolean equalProject = getProject().equals(other.getProject());
		boolean equalName = elementName.equals(other.elementName);
		return equalProject && equalName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + getProject().hashCode();
		result = prime * result + ((elementName == null) ? 0 : elementName.hashCode());
		return result;
	}

	@Override
	public void delete() {
		try {
			if (!resultFolder.isSynchronized(IResource.DEPTH_INFINITE)) {
				resultFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
			}

			ResultsView.reset(resultFolder);
			resultFolder.delete(true, null);
			// clear job id
			if (!JobsContainer.removeJobId(getProject(), jobId)) {
				DialogUtils
						.openError("There was an error while updating the project's job ids. The results of the corresponding id will be fetched again.");
			}
			// update navigator viewer
			SpotterProjectResults parent = (SpotterProjectResults) getParent();
			parent.refreshChildren();
			Activator.getDefault().getNavigatorViewer().refresh(parent);
		} catch (CoreException e) {
			String message = "Error while deleting result folder '" + resultFolder.getName() + "'!";
			DialogUtils.handleError(message, e);
		}
	}

	@Override
	public String getElementTypeName() {
		return ELEMENT_TYPE_NAME;
	}

}
