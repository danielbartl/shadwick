package dev.jbaby.shadwick.examples;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/** Direct responses for Studio navigations; listener and mutation requests keep Wicket defaults. */
final class StudioPageMapper extends MountedMapper {
    StudioPageMapper() { super("/studio", StudioPage.class); }

    @Override
    protected IRequestHandler processBookmarkable(Class<? extends IRequestablePage> pageClass, PageParameters parameters) {
        IRequestHandler handler = super.processBookmarkable(pageClass, parameters);
        var request = RequestCycle.get().getRequest();
        if (request instanceof WebRequest web && !web.isAjax()
                && request.getContainerRequest() instanceof HttpServletRequest servlet
                && "GET".equals(servlet.getMethod())
                && handler instanceof RenderPageRequestHandler render) {
            return new RenderPageRequestHandler(render.getPageProvider(), RenderPageRequestHandler.RedirectPolicy.NEVER_REDIRECT);
        }
        return handler;
    }
}
