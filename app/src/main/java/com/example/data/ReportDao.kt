package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    // Reports
    @Query("SELECT * FROM reports ORDER BY updatedAt DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :id")
    fun getReportByIdFlow(id: String): Flow<ReportEntity?>

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getReportById(id: String): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteReportById(id: String)

    // Issues
    @Query("SELECT * FROM issues WHERE reportId = :reportId ORDER BY issueNumber ASC")
    fun getIssuesForReport(reportId: String): Flow<List<IssueEntity>>

    @Query("SELECT * FROM issues WHERE reportId = :reportId ORDER BY issueNumber ASC")
    suspend fun getIssuesForReportSync(reportId: String): List<IssueEntity>

    @Query("SELECT * FROM issues WHERE id = :id")
    fun getIssueByIdFlow(id: String): Flow<IssueEntity?>

    @Query("SELECT * FROM issues WHERE id = :id")
    suspend fun getIssueById(id: String): IssueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: IssueEntity)

    @Update
    suspend fun updateIssue(issue: IssueEntity)

    @Query("DELETE FROM issues WHERE id = :id")
    suspend fun deleteIssueById(id: String)

    // Photos
    @Query("SELECT * FROM photos WHERE issueId = :issueId ORDER BY createdAt ASC")
    fun getPhotosForIssue(issueId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE reportId = :reportId")
    fun getPhotosForReport(reportId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE reportId = :reportId ORDER BY createdAt ASC")
    suspend fun getPhotosForReportSync(reportId: String): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id = :id")
    fun getPhotoByIdFlow(id: String): Flow<PhotoEntity?>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getPhotoById(id: String): PhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deletePhotoById(id: String)

    @Query("SELECT COUNT(*) FROM photos WHERE issueId = :issueId")
    fun getPhotoCountForIssue(issueId: String): Flow<Int>
}
