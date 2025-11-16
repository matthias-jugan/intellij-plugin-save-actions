package software.xdev.saveactions.core.filter;

import java.util.ArrayList;
import java.util.List;

import com.intellij.codeInsight.actions.VcsFacade;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.ChangedRangesInfo;


public class VcsFilter implements InspectionFilter
{
	private static final VcsFilter INSTANCE = new VcsFilter();
	
	public static VcsFilter get()
	{
		return INSTANCE;
	}
	
	@Override
	public List<ProblemDescriptor> filter(final List<ProblemDescriptor> elements, final PsiFile file)
	{
		final ChangedRangesInfo changedInfo = VcsFacade.getInstance().getChangedRangesInfo(file);
		if(changedInfo == null)
		{
			return new ArrayList<>(elements);
		}
		final List<TextRange> changedRanges = new ArrayList<>(changedInfo.allChangedRanges);
		if(changedInfo.insertedRanges != null)
		{
			changedRanges.addAll(changedInfo.insertedRanges);
		}
		return elements.stream().filter(element -> {
			final TextRange fixedRange = element.getPsiElement().getTextRange();
			return changedRanges.stream().anyMatch(changed -> changed.intersects(fixedRange));
		}).toList();
	}
}
