/*
 * ====================================================================
 * Copyright (c) 2005-2006 Sventon Project. All rights reserved.
 *
 * This software is licensed as described in the file LICENSE, which
 * you should have received as part of this distribution. The terms
 * are also available at http://sventon.berlios.de.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package de.berlios.sventon.web.ctrl;

import de.berlios.sventon.diff.DiffAction;
import de.berlios.sventon.diff.DiffException;
import de.berlios.sventon.diff.SourceLine;
import de.berlios.sventon.web.command.DiffCommand;
import de.berlios.sventon.web.command.SVNBaseCommand;
import de.berlios.sventon.web.model.DiffTableDataRow;
import de.berlios.sventon.web.model.UserContext;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNFileRevision;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNRevision;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The DiffPreviousController generates a diff between a given entry
 * and its previous entry in history.
 *
 * @author jesper@users.berlios.de
 */
public class DiffPreviousController extends AbstractSVNTemplateController implements Controller {

  /**
   * {@inheritDoc}
   */
  protected ModelAndView svnHandle(final SVNRepository repository, final SVNBaseCommand svnCommand,
                                   final SVNRevision revision, final UserContext userContext,
                                   final HttpServletRequest request, final HttpServletResponse response,
                                   final BindException exception) throws Exception {

    final long commitRev = ServletRequestUtils.getLongParameter(request, "commitrev");
    logger.debug("Diffing file contents for: " + svnCommand);
    logger.debug("committed-rev: " + commitRev);
    final Map<String, Object> model = new HashMap<String, Object>();

    try {
      //TODO: Solve this issue in a better way?
      if (SVNNodeKind.NONE == getRepositoryService().getNodeKind(repository, svnCommand.getPath(), commitRev)) {
        throw new DiffException("Entry has no history in current branch");
      }
      final List<SVNFileRevision> revisions = getRepositoryService().getFileRevisions(repository, svnCommand.getPath(),
          commitRev);

      final DiffCommand diffCommand = new DiffCommand(revisions);
      model.put("diffCommand", diffCommand);
      logger.debug("Using: " + diffCommand);

      final List<SourceLine[]> sourceLines = getRepositoryService().diff(
          repository, diffCommand, getConfiguration().getInstanceConfiguration(svnCommand.getName()));
      final List<DiffTableDataRow> tableRows = new ArrayList<DiffTableDataRow>();
      int diffCounter = 0;
      int rowNumber = 0;
      for (SourceLine[] sourceLine : sourceLines) {
        String diffAnchor = "";
        String nextDiffAnchor = "";
        if (sourceLine[0].getAction() != DiffAction.UNCHANGED) {
          diffAnchor = "diff" + diffCounter;
          nextDiffAnchor = "diff" + ++diffCounter;
        }
        tableRows.add(new DiffTableDataRow(sourceLine, ++rowNumber, diffAnchor, nextDiffAnchor));
      }
      model.put("tableRows", tableRows);

    } catch (DiffException dex) {
      model.put("diffException", dex.getMessage());
    }

    return new ModelAndView("diff", model);
  }

}