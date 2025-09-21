package com.webdws.repository;

import com.webdws.model.XmlDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * XmlDocumentRepository - Data Access Layer for XML Documents
 * 
 * This repository interface provides:
 * - Standard CRUD operations through JpaRepository inheritance
 * - Custom query methods for document retrieval
 * - Integration with eXist-db ID mapping
 * - Pagination support for large document collections
 * - Search functionality by document name
 */
@Repository
public interface XmlDocumentRepository extends JpaRepository<XmlDocument, Long> {
    
    Optional<XmlDocument> findByExistDbId(String existDbId);
    
    @Query("SELECT x FROM XmlDocument x WHERE x.name LIKE %:name%")
    Page<XmlDocument> findByNameContaining(@Param("name") String name, Pageable pageable);
    
    @Query("SELECT COUNT(x) FROM XmlDocument x")
    long countAllDocuments();
}
