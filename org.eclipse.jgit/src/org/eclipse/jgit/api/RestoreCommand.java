package org.eclipse.jgit.api;

import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.dircache.*;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.*;
//import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

//import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;

import static org.eclipse.jgit.treewalk.TreeWalk.OperationType.CHECKOUT_OP;

/**
 * A class used to execute a {@code Restore} command. It has setters for all
 * supported options and arguments of this command and a {@link #call()} method
 * to finally execute the command. Each instance of this class should only be
 * used for one invocation of the command (means: one call to {@link #call()})
 *
 * @see <a href="http://www.kernel.org/pub/software/scm/git/docs/git-add.html"
 *      >Git documentation about Add</a>
 */

public class RestoreCommand extends GitCommand<DirCache> {


    private final ArrayList<String> filePatterns;

    private boolean toIndex;
    private boolean toWorkTree;
    private String source;

    public RestoreCommand setToIndex(boolean flag){
        this.toIndex = flag;
        return this;
    }

    public RestoreCommand setToWorkTree(boolean flag){
        this.toWorkTree = flag;
        return this;
    }

    protected RestoreCommand(Repository repo){
        super(repo);
        filePatterns = new ArrayList<>();
    }

    public void addFilePattern(String filePattern){
        checkCallable();
        filePatterns.add(filePattern);
    }

    public RestoreCommand setSource(String source){
        this.source = source;
        return this;
    }

    private ObjectId getSourceObjectId() throws IOException, RefNotFoundException, RestoreFromIndexException {
        if (source != null){
            //if the source is given as part of the command options, try to get the objectId of the source
            ObjectId result = repo.resolve(source);
            if (result == null){
                throw new RefNotFoundException(MessageFormat.format(JGitText.get().refNotResolved, source));
            }
            return result;
        }

        if (toIndex){
            //no source is specified in the command options and if the index is to be updated, HEAD should be the source
            //null will be returned when the repository has no commits
            return repo.resolve(Constants.HEAD);
        }

        //restore all the paths from INDEX
        throw new RestoreFromIndexException("Restore From INDEX");
    }

    private void restorePathsFromCommit(TreeWalk treeWalk,DirCache dc,RevCommit commit) throws IOException {
        treeWalk.addTree(commit.getTree());
        final ObjectReader r = treeWalk.getObjectReader();
        DirCacheEditor editor = null;
        if (toIndex){
            editor = dc.editor();
            DirCacheIterator dci = new DirCacheIterator(dc);
            treeWalk.addTree(dci);
        }


        while(treeWalk.next()){
            final ObjectId blobId = treeWalk.getObjectId(0);
            final FileMode mode = treeWalk.getFileMode(0);
            final CoreConfig.EolStreamType eolStreamType = treeWalk
                    .getEolStreamType(CHECKOUT_OP);
            final String filterCommand = treeWalk
                    .getFilterCommand(Constants.ATTR_FILTER_TYPE_SMUDGE);
            final String path = treeWalk.getPathString();
            editor.add(new DirCacheEditor.PathEdit(path) {
                @Override
                public void apply(DirCacheEntry ent) {
                    if (ent.getStage() != DirCacheEntry.STAGE_0) {
                        ent.setStage(DirCacheEntry.STAGE_0);
                    }
                    ent.setObjectId(blobId);
                    ent.setFileMode(mode);

                }
            });
        }
    }

    private DirCache restorePaths(boolean addAll) throws IOException {
        DirCache dc = repo.lockDirCache();
        try (RevWalk revWalk = new RevWalk(repo);
             TreeWalk treeWalk = new TreeWalk(repo,
                     revWalk.getObjectReader())){
            treeWalk.setRecursive(true);
            if (!addAll){
                treeWalk.setFilter(PathFilterGroup.createFromStrings(filePatterns));
            }
            ObjectId restoreSourceId = getSourceObjectId();
            if (restoreSourceId == null){
                //just return silently as the repository has no commits yet
                return null;
            }
            RevCommit commit = revWalk.parseCommit(restoreSourceId);

            restorePathsFromCommit(treeWalk,dc,commit);

            //restore to worktree/index/both from the commit

        } catch (RefNotFoundException e) {
            //this is literally an error scenario
        } catch (RestoreFromIndexException e) {
            //restore to worktree from INDEX
        }

    }

    @Override
    public DirCache call() throws GitAPIException{
        checkCallable();
//        DirCache dc = repo.lockDirCache();
        boolean addAll = filePatterns.contains(".");

        return restorePaths(addAll);


//        try {
//
//            boolean addAll = filepatterns.contains(".");
//
//
//            ObjectId restoreSourceId = getSourceObjectId();
//            if (restoreSourceId == null){
//                //just return silently as the repository has no commits yet
//                return null;
//            }
//
//        } catch(RestoreFromIndexException rie){
//            //Checkout worktree from INDEX
//        } catch (RefNotFoundException re){
//            //this is literally an error scenario
//        } catch (IOException ioe){
//            //this is literally an error scenario
//        }
    }
}
